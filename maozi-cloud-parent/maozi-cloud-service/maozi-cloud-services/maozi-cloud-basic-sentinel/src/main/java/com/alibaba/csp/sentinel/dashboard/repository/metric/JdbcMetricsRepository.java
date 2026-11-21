/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 将指标数据持久化到 MySQL
 * <p>
 * 表（含索引）通过 {@code schema.sql} 在启动时自动创建，
 * 数据库由 JDBC url 的 {@code createDatabaseIfNotExist=true} 参数自动创建
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class JdbcMetricsRepository implements MetricsRepository<MetricEntity> {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(JdbcMetricsRepository.class);

    /** 单条语句最多删除的行数，避免单次 DELETE 执行过久 */
    private static final long MAX_BATCH_DELETE_ROWS = 5000;

    /** 插入指标行，唯一键（app+resource+timestamp）冲突时更新各指标列 */
    private static final String UPSERT_SQL =
        "INSERT INTO sentinel_metric (gmt_create, gmt_modified, app, `timestamp`, resource, "
            + "pass_qps, success_qps, block_qps, exception_qps, rt, `count`) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE gmt_modified = VALUES(gmt_modified), pass_qps = VALUES(pass_qps), "
            + "success_qps = VALUES(success_qps), block_qps = VALUES(block_qps), "
            + "exception_qps = VALUES(exception_qps), rt = VALUES(rt), `count` = VALUES(`count`)";

    /** 按时间戳升序加载一个 app+resource 在时间区间内的指标 */
    private static final String QUERY_SQL =
        "SELECT id, gmt_create, gmt_modified, app, `timestamp`, resource, "
            + "pass_qps, success_qps, block_qps, exception_qps, rt, `count` "
            + "FROM sentinel_metric WHERE app = ? AND resource = ? AND `timestamp` BETWEEN ? AND ? "
            + "ORDER BY `timestamp` ASC";

    /** 按资源汇总一个应用在窗口内的阻塞/通过 QPS，阻塞最多的资源排在前 */
    private static final String LIST_RESOURCES_SQL =
        "SELECT resource, SUM(block_qps) AS block_qps_total, SUM(pass_qps) AS pass_qps_total "
            + "FROM sentinel_metric WHERE app = ? AND `timestamp` >= ? AND `timestamp` <= ? "
            + "GROUP BY resource ORDER BY block_qps_total DESC, pass_qps_total DESC";

    /** 每次执行最多删除 {@link #MAX_BATCH_DELETE_ROWS} 行早于指定时间戳的数据 */
    private static final String DELETE_EXPIRED_SQL =
        "DELETE FROM sentinel_metric WHERE `timestamp` < ? LIMIT " + MAX_BATCH_DELETE_ROWS;

    /** 操作指标表的 Spring JDBC 访问器 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 指标数据保留天数，过期数据每天清理一次；非正数表示关闭清理
     */
    @Value("${sentinel.metric.retention-days:30}")
    private int retentionDays;

    /**
     * 指标聚合粒度 N 秒（通过 {@code sentinel.metric.aggregate-seconds} 配置）。
     * N &gt; 1 时，拉取到的 N 秒原始秒级指标合并为一条记录，时间戳对齐到
     * N 秒网格，存储量缩小 N 倍，图表每 N 秒展示一个点。QPS 字段保存的是
     * N 秒窗口内的请求总量（非秒均值），如 N=1000 且窗口内有 3 个请求则
     * pass_qps=3；{@code rt} 保持 {@link MetricEntity#getRt()} 的总 RT 语义，
     * 读取时 {@code rt / successQps} 即为请求加权平均 RT，任意 N 均成立。
     * 默认值 1 保持原始的秒级存储
     */
    @Value("${sentinel.metric.aggregate-seconds:1}")
    private int aggregateSeconds;

    /**
     * 超过该时长的聚合桶从内存淘汰。拉取器重启后最多回拉 15 秒数据，
     * 15 分钟远大于任何迟到数据
     */
    private static final long BUCKET_EVICT_MS = 1000 * 60 * 15;

    /**
     * 后台刷盘存在未落盘秒数的桶的调度间隔。桶在每次 {@link #saveAll} 时已同步
     * 落盘，该调度仅兜底尾部数据（流量停止后拉取器不再调用 {@code saveAll}）
     * 并淘汰过期桶，短周期保证尾部数据近实时可见
     */
    private static final int BUCKET_FLUSH_INTERVAL_SECONDS = 2;

    /** 每日过期指标清理任务的初始延迟（分钟） */
    private static final int CLEAN_INITIAL_DELAY_MINUTES = 5;

    /** 每日过期指标清理任务的执行周期（分钟），即一天一次 */
    private static final int CLEAN_PERIOD_MINUTES = 1440;

    /**
     * 一个 N 秒桶的内存累加器：按秒实体折叠出的累计值与已折叠的秒数。
     * 拉取器的窗口不重叠推进，重复到达的秒只能来自调度停顿后的回拉
     * （见 {@code MetricFetcher} 的 {@code MAX_LAST_FETCH_INTERVAL_MS}），
     * 跳过这类重复秒避免重复计数
     * <p>
     * {@code flushedSeconds} 记录当前累计值中已落盘的秒数；由于
     * {@code foldedSeconds} 只增不减，比较两者大小即为无竞态的"脏"判定，
     * 保证每批新秒数只触发一次写入
     */
    private static final class BucketAccumulator {
        /** 桶的累计值，保存在一个时间戳为桶起始时间的指标实体中 */
        final MetricEntity entity;
        /** 已折叠进累计值的秒（毫秒、截断后），同时用于去重 */
        final Set<Long> foldedSeconds = ConcurrentHashMap.newKeySet();
        /** 累计值中已落盘的秒数 */
        volatile int flushedSeconds;

        /** 基于一个初始化（全零）的桶实体创建累加器 */
        BucketAccumulator(MetricEntity entity) {
            this.entity = entity;
        }
    }

    /**
     * {@code app__resource__bucketStartMs -> 累计中的桶}，
     * 仅在 {@code aggregateSeconds > 1} 时使用
     */
    private final ConcurrentHashMap<String, BucketAccumulator> bucketBuffer = new ConcurrentHashMap<>();

    /** 每日过期指标行清理的调度器；清理关闭时为 null */
    private ScheduledExecutorService cleanerScheduler;

    /** 聚合桶尾部刷盘的调度器；聚合关闭时为 null */
    private ScheduledExecutorService bucketFlushScheduler;

    /** 将 sentinel_metric 表的一行映射为 {@link MetricEntity} */
    private final RowMapper<MetricEntity> metricRowMapper = new RowMapper<MetricEntity>() {
        /** 将当前结果集行映射为指标实体 */
        @Override
        public MetricEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            MetricEntity entity = new MetricEntity();
            entity.setId(rs.getLong("id"));
            entity.setGmtCreate(new Date(rs.getTimestamp("gmt_create").getTime()));
            entity.setGmtModified(new Date(rs.getTimestamp("gmt_modified").getTime()));
            entity.setApp(rs.getString("app"));
            entity.setTimestamp(new Date(rs.getLong("timestamp")));
            entity.setResource(rs.getString("resource"));
            entity.setPassQps(rs.getLong("pass_qps"));
            entity.setSuccessQps(rs.getLong("success_qps"));
            entity.setBlockQps(rs.getLong("block_qps"));
            entity.setExceptionQps(rs.getLong("exception_qps"));
            entity.setRt(rs.getDouble("rt"));
            entity.setCount(rs.getInt("count"));
            return entity;
        }
    };

    /** 使用操作指标表的 JDBC 访问器创建仓储 */
    public JdbcMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 启动后台调度器：聚合开启时启动桶尾部刷盘，
     * retentionDays 为正数时启动每日过期指标清理
     */
    @PostConstruct
    public void init() {
        if (aggregateSeconds > 1) {
            bucketFlushScheduler = new ScheduledThreadPoolExecutor(1, runnable -> {
                Thread thread = new Thread(runnable, "sentinel-metric-bucket-flusher");
                thread.setDaemon(true);
                return thread;
            });
            bucketFlushScheduler.scheduleWithFixedDelay(this::flushDirtyBucketsQuietly,
                BUCKET_FLUSH_INTERVAL_SECONDS, BUCKET_FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
            logger.info("Metric aggregation enabled: {} seconds per bucket, buckets are persisted as data arrives "
                + "and the tail is flushed every {} seconds", aggregateSeconds, BUCKET_FLUSH_INTERVAL_SECONDS);
        }
        if (retentionDays <= 0) {
            logger.info("Metric retention cleanup is disabled (sentinel.metric.retention-days={})", retentionDays);
            return;
        }
        cleanerScheduler = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "sentinel-metric-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        cleanerScheduler.scheduleAtFixedRate(this::cleanExpiredMetrics,
            CLEAN_INITIAL_DELAY_MINUTES, CLEAN_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    /** 关闭调度器，并尽力把内存中残留的聚合桶刷盘 */
    @PreDestroy
    public void destroy() {
        if (cleanerScheduler != null) {
            cleanerScheduler.shutdownNow();
        }
        if (bucketFlushScheduler != null) {
            bucketFlushScheduler.shutdownNow();
        }
        // 尽力刷盘内存中残留的聚合桶
        if (aggregateSeconds > 1 && !bucketBuffer.isEmpty()) {
            List<MetricEntity> remaining = new ArrayList<>(bucketBuffer.size());
            for (BucketAccumulator accumulator : bucketBuffer.values()) {
                remaining.add(normalizeBucket(accumulator));
            }
            doUpsert(remaining);
        }
    }

    /** 通过 {@link #saveAll} 保存单个指标实体；入参为 null 或应用名为空时忽略 */
    @Override
    public void save(MetricEntity metric) {
        if (metric == null || StringUtil.isBlank(metric.getApp())) {
            return;
        }
        List<MetricEntity> metrics = new ArrayList<>(1);
        metrics.add(metric);
        saveAll(metrics);
    }

    /**
     * 持久化给定指标：先过滤 null 实体、应用名为空或时间戳为 null 的实体，
     * 再依据 {@link #aggregateSeconds} 折叠进 N 秒桶或直接批量写入
     */
    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        List<MetricEntity> validMetrics = new ArrayList<>();
        for (MetricEntity metric : metrics) {
            if (metric == null || StringUtil.isBlank(metric.getApp()) || metric.getTimestamp() == null) {
                continue;
            }
            validMetrics.add(metric);
        }
        if (validMetrics.isEmpty()) {
            return;
        }
        if (aggregateSeconds > 1) {
            bufferAndPersistBuckets(validMetrics);
        } else {
            doUpsert(validMetrics);
        }
    }

    /**
     * 将秒级实体合并进 N 秒桶（内存中）并立即持久化。拉取器按约 6 秒窗口拉取，
     * 一个桶会跨多次调用渐进折叠；窗口回退后重复拉取的秒只折叠一次。
     * 每批新秒数立即以 upsert 快照（幂等全量替换）落盘，
     * 桶收集期间数据即可近实时可见
     */
    private void bufferAndPersistBuckets(List<MetricEntity> metrics) {
        long bucketMs = aggregateSeconds * 1000L;
        for (MetricEntity metric : metrics) {
            long metricTime = metric.getTimestamp().getTime();
            long bucketStart = metricTime / bucketMs * bucketMs;
            String key = metric.getApp() + "__" + metric.getResource() + "__" + bucketStart;
            long second = metricTime / 1000 * 1000;
            bucketBuffer.compute(key, (k, acc) -> {
                BucketAccumulator accumulator = acc;
                if (accumulator == null) {
                    MetricEntity bucket = new MetricEntity();
                    bucket.setApp(metric.getApp());
                    bucket.setResource(metric.getResource());
                    bucket.setTimestamp(new Date(bucketStart));
                    bucket.setGmtCreate(metric.getGmtCreate());
                    bucket.setPassQps(0L);
                    bucket.setBlockQps(0L);
                    bucket.setRtAndSuccessQps(0, 0L);
                    bucket.setExceptionQps(0L);
                    bucket.setCount(0);
                    accumulator = new BucketAccumulator(bucket);
                }
                // 窗口回退后重复拉取的秒只折叠一次，避免桶被重复计数
                if (!accumulator.foldedSeconds.add(second)) {
                    return accumulator;
                }
                MetricEntity bucket = accumulator.entity;
                bucket.addPassQps(metric.getPassQps());
                bucket.addBlockQps(metric.getBlockQps());
                // 实体的 rt 本身就是总 RT（平均 RT * successQps），桶内直接累加即可；
                // 不能使用 addRtAndSuccessQps，否则会把总 RT 再乘一次 successQps
                bucket.setRt(bucket.getRt() + metric.getRt());
                bucket.setSuccessQps(bucket.getSuccessQps() + metric.getSuccessQps());
                bucket.addExceptionQps(metric.getExceptionQps());
                bucket.addCount(metric.getCount());
                return accumulator;
            });
        }
        flushDirtyBuckets();
    }

    /**
     * 持久化存在未落盘秒数的桶（含仍在收集与已关闭的桶）并淘汰过期桶。
     * 每次折叠后同步执行，且由后台刷盘调度兜底，流量停止、{@link #saveAll}
     * 不再被调用后尾部数据仍能落库
     * <p>
     * 桶的落盘行是已折叠内容的 upsert 快照（QPS 字段为已折叠秒数的请求总量），
     * 桶收集期间数值近实时增长；每批新秒数恰好触发一次写入，
     * 同步刷盘与调度刷盘的竞争最多造成一次幂等的多余写入，不会丢数据
     */
    private void flushDirtyBuckets() {
        long now = System.currentTimeMillis();
        List<BucketAccumulator> dirty = new ArrayList<>();
        Iterator<Map.Entry<String, BucketAccumulator>> iterator = bucketBuffer.entrySet().iterator();
        while (iterator.hasNext()) {
            BucketAccumulator accumulator = iterator.next().getValue();
            long age = now - accumulator.entity.getTimestamp().getTime();
            if (age >= BUCKET_EVICT_MS) {
                iterator.remove();
                continue;
            }
            if (accumulator.foldedSeconds.size() > accumulator.flushedSeconds) {
                dirty.add(accumulator);
            }
        }
        if (dirty.isEmpty()) {
            return;
        }
        int dirtyCount = dirty.size();
        List<Integer> snapshots = new ArrayList<>(dirtyCount);
        List<MetricEntity> toPersist = new ArrayList<>(dirtyCount);
        for (BucketAccumulator accumulator : dirty) {
            snapshots.add(accumulator.foldedSeconds.size());
            toPersist.add(normalizeBucket(accumulator));
        }
        doUpsert(toPersist);
        for (int i = 0; i < dirtyCount; i++) {
            dirty.get(i).flushedSeconds = snapshots.get(i);
        }
    }

    /**
     * 刷盘调度的 {@link #flushDirtyBuckets} 包装：吞掉并记录异常，
     * 保证调度永不中断
     */
    private void flushDirtyBucketsQuietly() {
        try {
            flushDirtyBuckets();
        } catch (Exception e) {
            // 数据库异常也不能中断刷盘调度
            logger.error("Error when flushing aggregation buckets", e);
        }
    }

    /**
     * 构建一个桶的落盘快照。QPS 字段为已折叠秒数的请求总量，即页面展示的
     * "N 秒窗口内的 QPS"：N=1000 且窗口内有 3 个请求时 pass_qps=3（非秒均值）。
     * {@code rt} 保持总 RT 语义（平均 RT * successQps），读取时
     * {@code rt / successQps} 即为请求加权平均 RT，任意 N 均成立。
     * 内存累加器保留累计值以便继续折叠
     */
    private MetricEntity normalizeBucket(BucketAccumulator accumulator) {
        MetricEntity bucket = accumulator.entity;
        MetricEntity normalized = new MetricEntity();
        normalized.setApp(bucket.getApp());
        normalized.setResource(bucket.getResource());
        normalized.setTimestamp(bucket.getTimestamp());
        normalized.setGmtCreate(bucket.getGmtCreate());
        normalized.setGmtModified(new Date());
        // N 秒窗口总量：不做秒级折算，窗口内 3 个请求即存 3
        normalized.setPassQps(bucket.getPassQps());
        normalized.setSuccessQps(bucket.getSuccessQps());
        normalized.setBlockQps(bucket.getBlockQps());
        normalized.setExceptionQps(bucket.getExceptionQps());
        // rt 保持总 RT 语义（平均 RT * successQps），读取时 rt / successQps 即请求加权平均 RT
        normalized.setRt(bucket.getRt());
        normalized.setCount(bucket.getCount());
        return normalized;
    }

    /**
     * 用 {@link #UPSERT_SQL} 批量写入实体；捕获并记录数据库异常，
     * 保证指标拉取循环永不中断
     */
    private void doUpsert(List<MetricEntity> validMetrics) {
        try {
            jdbcTemplate.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
                /** 将第 i 个指标的各字段绑定到 upsert 语句 */
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MetricEntity metric = validMetrics.get(i);
                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    Date gmtCreate = metric.getGmtCreate();
                    Long passQps = metric.getPassQps();
                    Long successQps = metric.getSuccessQps();
                    Long blockQps = metric.getBlockQps();
                    Long exceptionQps = metric.getExceptionQps();
                    ps.setTimestamp(1, gmtCreate == null ? now : new Timestamp(gmtCreate.getTime()));
                    ps.setTimestamp(2, now);
                    ps.setString(3, metric.getApp());
                    ps.setLong(4, metric.getTimestamp().getTime());
                    ps.setString(5, metric.getResource());
                    ps.setLong(6, passQps == null ? 0 : passQps);
                    ps.setLong(7, successQps == null ? 0 : successQps);
                    ps.setLong(8, blockQps == null ? 0 : blockQps);
                    ps.setLong(9, exceptionQps == null ? 0 : exceptionQps);
                    ps.setDouble(10, metric.getRt());
                    ps.setInt(11, metric.getCount());
                }

                /** 返回待写入的指标数量 */
                @Override
                public int getBatchSize() {
                    return validMetrics.size();
                }
            });
        } catch (Exception e) {
            // 数据库异常也不能中断指标拉取循环
            logger.error("Error when saving metrics to MySQL, size={}", validMetrics.size(), e);
        }
    }

    /**
     * 加载一个 app+resource 在 [startTime, endTime] 区间（双边均含）内的指标，
     * 按时间戳升序排列；入参为空或数据库异常时返回空列表
     */
    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource,
                                                           long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<>();
        if (StringUtil.isBlank(app) || StringUtil.isBlank(resource)) {
            return results;
        }
        try {
            return jdbcTemplate.query(QUERY_SQL, metricRowMapper, app, resource, startTime, endTime);
        } catch (Exception e) {
            logger.error("Error when querying metrics from MySQL, app={}, resource={}", app, resource, e);
            return results;
        }
    }

    /**
     * 列出应用在 [startTime, endTime] 区间内有指标数据的资源，
     * 按阻塞 QPS 总和、通过 QPS 总和降序排列；应用名为空或数据库异常时返回空列表
     */
    @Override
    public List<String> listResourcesOfApp(String app, long startTime, long endTime) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        try {
            return jdbcTemplate.query(LIST_RESOURCES_SQL,
                (rs, rowNum) -> rs.getString("resource"), app, startTime, endTime);
        } catch (Exception e) {
            logger.error("Error when listing resources of app from MySQL, app={}", app, e);
            return results;
        }
    }

    /**
     * 按 {@link #MAX_BATCH_DELETE_ROWS} 分批删除早于 {@link #retentionDays} 天的
     * 指标行，直到单批影响行数小于批大小为止
     */
    private void cleanExpiredMetrics() {
        try {
            long expireTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays);
            long totalDeleted = 0;
            while (jdbcTemplate.update(DELETE_EXPIRED_SQL, expireTime) >= MAX_BATCH_DELETE_ROWS) {
                totalDeleted += MAX_BATCH_DELETE_ROWS;
            }
            if (totalDeleted > 0) {
                logger.info("Cleaned expired metrics older than {} days, at least {} rows deleted",
                    retentionDays, totalDeleted);
            }
        } catch (Exception e) {
            logger.error("Error when cleaning expired metrics", e);
        }
    }
}
