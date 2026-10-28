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
 * Persist metrics data into MySQL.
 * <p>
 * The table (with indexes) is auto-created on startup via {@code schema.sql},
 * while the database is auto-created by the {@code createDatabaseIfNotExist=true}
 * JDBC url parameter.
 *
 * @author maozi
 */
@Component
public class JdbcMetricsRepository implements MetricsRepository<MetricEntity> {

    private static final Logger logger = LoggerFactory.getLogger(JdbcMetricsRepository.class);

    /** Max rows deleted per statement, to keep each DELETE short. */
    private static final long MAX_BATCH_DELETE_ROWS = 5000;

    /** Inserts a metric row, or updates the metric columns on duplicate key (app+resource+timestamp). */
    private static final String UPSERT_SQL =
        "INSERT INTO sentinel_metric (gmt_create, gmt_modified, app, `timestamp`, resource, "
            + "pass_qps, success_qps, block_qps, exception_qps, rt, `count`) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE gmt_modified = VALUES(gmt_modified), pass_qps = VALUES(pass_qps), "
            + "success_qps = VALUES(success_qps), block_qps = VALUES(block_qps), "
            + "exception_qps = VALUES(exception_qps), rt = VALUES(rt), `count` = VALUES(`count`)";

    /** Loads the metrics of one app+resource within a timestamp range, ascending by timestamp. */
    private static final String QUERY_SQL =
        "SELECT id, gmt_create, gmt_modified, app, `timestamp`, resource, "
            + "pass_qps, success_qps, block_qps, exception_qps, rt, `count` "
            + "FROM sentinel_metric WHERE app = ? AND resource = ? AND `timestamp` BETWEEN ? AND ? "
            + "ORDER BY `timestamp` ASC";

    /** Sums block/pass QPS per resource of one app in a window, most blocked resources first. */
    private static final String LIST_RESOURCES_SQL =
        "SELECT resource, SUM(block_qps) AS block_qps_total, SUM(pass_qps) AS pass_qps_total "
            + "FROM sentinel_metric WHERE app = ? AND `timestamp` >= ? AND `timestamp` <= ? "
            + "GROUP BY resource ORDER BY block_qps_total DESC, pass_qps_total DESC";

    /** Deletes at most {@link #MAX_BATCH_DELETE_ROWS} rows older than a timestamp per execution. */
    private static final String DELETE_EXPIRED_SQL =
        "DELETE FROM sentinel_metric WHERE `timestamp` < ? LIMIT " + MAX_BATCH_DELETE_ROWS;

    /** Spring JDBC accessor used for all statements against the metric table. */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Days to keep metric data, expired data is cleaned up once a day. Non-positive value disables cleanup.
     */
    @Value("${sentinel.metric.retention-days:30}")
    private int retentionDays;

    /**
     * Metric aggregation granularity N in seconds (configurable via
     * {@code sentinel.metric.aggregate-seconds}). When N &gt; 1, the N seconds of raw
     * per-second metrics pulled by the fetcher are merged into ONE persisted row whose
     * timestamp is aligned to the N-second grid, so the storage volume shrinks N times
     * and the chart displays one point per N seconds. The QPS fields carry the request
     * TOTALS inside the N-second window (not a per-second average): with N=1000 and 3
     * requests in the window the row shows pass_qps=3; {@code rt} keeps the "total RT"
     * semantics of {@link MetricEntity#getRt()}, so the average RT read as
     * {@code rt / successQps} is the request-weighted mean for any N. Default 1 keeps
     * the original per-second storage.
     */
    @Value("${sentinel.metric.aggregate-seconds:1}")
    private int aggregateSeconds;

    /**
     * Aggregation buckets older than this are evicted from memory. The fetcher re-pulls
     * at most 15 seconds after a restart, so 15 minutes is far beyond any late arrival.
     */
    private static final long BUCKET_EVICT_MS = 1000 * 60 * 15;

    /**
     * Interval of the background flush of buckets with unflushed seconds. Buckets are
     * already persisted inline on every {@link #saveAll}; this schedule only covers the
     * tail (the fetcher stops calling {@code saveAll} once traffic stops) and evicts
     * stale buckets, so a short period keeps the tail visible almost in real time.
     */
    private static final int BUCKET_FLUSH_INTERVAL_SECONDS = 2;

    /**
     * In-memory accumulator of one N-second bucket: the running totals folded from the
     * per-second entities, plus the seconds already folded. The fetcher advances its
     * windows without overlap, so a second arriving again can only come from a re-pull
     * after a scheduler stall (see {@code MAX_LAST_FETCH_INTERVAL_MS} in
     * {@code MetricFetcher}); such duplicates are skipped to avoid double counting.
     * <p>
     * {@code flushedSeconds} records how many seconds of the current totals have already
     * been persisted; since {@code foldedSeconds} only grows, comparing both sizes gives
     * a race-free "dirty" test so each batch of new seconds triggers exactly one write.
     */
    private static final class BucketAccumulator {
        /** Running totals of the bucket, kept in a metric entity whose timestamp is the bucket start. */
        final MetricEntity entity;
        /** Seconds (ms, trimmed) already folded into the totals; also guards against duplicates. */
        final Set<Long> foldedSeconds = ConcurrentHashMap.newKeySet();
        /** Number of seconds of the totals that have already been persisted. */
        volatile int flushedSeconds;

        /** Creates an accumulator around an initialized (all-zero) bucket entity. */
        BucketAccumulator(MetricEntity entity) {
            this.entity = entity;
        }
    }

    /**
     * {@code app__resource__bucketStartMs -> accumulated bucket},
     * only used when {@code aggregateSeconds > 1}.
     */
    private final ConcurrentHashMap<String, BucketAccumulator> bucketBuffer = new ConcurrentHashMap<>();

    /** Scheduler of the daily cleanup of expired metric rows; null when retention is disabled. */
    private ScheduledExecutorService cleanerScheduler;

    /** Scheduler of the periodic tail flush of aggregation buckets; null when aggregation is off. */
    private ScheduledExecutorService bucketFlushScheduler;

    /** Maps one row of the sentinel_metric table to a {@link MetricEntity}. */
    private final RowMapper<MetricEntity> metricRowMapper = new RowMapper<MetricEntity>() {
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

    public JdbcMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Starts the background schedulers: the bucket tail flush when aggregation is enabled,
     * and the daily expired-metric cleanup when retentionDays is positive.
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
        // Initial delay 5 minutes, then run once a day (1440 minutes).
        cleanerScheduler.scheduleAtFixedRate(this::cleanExpiredMetrics, 5, 1440, TimeUnit.MINUTES);
    }

    /** Shuts down the schedulers and best-effort flushes the aggregation buckets still in memory. */
    @PreDestroy
    public void destroy() {
        if (cleanerScheduler != null) {
            cleanerScheduler.shutdownNow();
        }
        if (bucketFlushScheduler != null) {
            bucketFlushScheduler.shutdownNow();
        }
        // Best-effort flush of the aggregation buckets that are still in memory.
        if (aggregateSeconds > 1 && !bucketBuffer.isEmpty()) {
            List<MetricEntity> remaining = new ArrayList<>(bucketBuffer.size());
            for (BucketAccumulator accumulator : bucketBuffer.values()) {
                remaining.add(normalizeBucket(accumulator));
            }
            doUpsert(remaining);
        }
    }

    /** Saves a single metric entity via {@link #saveAll}; null or blank-app input is ignored. */
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
     * Persists the given metrics: filters out null entities and entities with a blank app
     * or null timestamp, then either folds them into N-second buckets or upserts them
     * directly, depending on {@link #aggregateSeconds}.
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
     * Merge the incoming per-second entities into N-second buckets (in memory) and persist
     * the buckets right away. The fetcher pulls in ~6s windows, so one bucket is folded
     * incrementally across several calls; a second pulled again after a window rollback is
     * folded only once. Every batch of new seconds is persisted immediately as an upsert
     * snapshot (idempotent full-state replacement), so the data becomes visible in near
     * real time while the bucket is still collecting.
     */
    private void bufferAndPersistBuckets(List<MetricEntity> metrics) {
        long bucketMs = aggregateSeconds * 1000L;
        for (MetricEntity metric : metrics) {
            long bucketStart = metric.getTimestamp().getTime() / bucketMs * bucketMs;
            String key = metric.getApp() + "__" + metric.getResource() + "__" + bucketStart;
            long second = metric.getTimestamp().getTime() / 1000 * 1000;
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
                // 窗口回退重拉到的重复秒只折叠一次, 防止桶值双倍计数
                if (!accumulator.foldedSeconds.add(second)) {
                    return accumulator;
                }
                MetricEntity bucket = accumulator.entity;
                bucket.addPassQps(metric.getPassQps());
                bucket.addBlockQps(metric.getBlockQps());
                // 实体的 rt 已是总 RT(avgRt*successQps), 桶累积直接相加;
                // 不能走 addRtAndSuccessQps, 它会把传入值当平均 RT 再乘一次 successQps
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
     * Persist the buckets that have unflushed seconds (both the still-open one and the
     * closed ones) and evict the stale ones. Runs inline after every folding and on the
     * background flush schedule, so tail data still lands in MySQL after traffic stops
     * and {@link #saveAll} is no longer invoked.
     * <p>
     * The persisted row of a bucket is the upsert snapshot of what has been folded so far
     * (QPS fields carrying the request totals of the folded seconds), so the values grow
     * in near real time while the bucket is still collecting, and each batch of new
     * seconds triggers exactly one write (races between the inline flush and the schedule
     * only cost an idempotent extra write, never a lost one).
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
        List<Integer> snapshots = new ArrayList<>(dirty.size());
        List<MetricEntity> toPersist = new ArrayList<>(dirty.size());
        for (BucketAccumulator accumulator : dirty) {
            snapshots.add(accumulator.foldedSeconds.size());
            toPersist.add(normalizeBucket(accumulator));
        }
        doUpsert(toPersist);
        for (int i = 0; i < dirty.size(); i++) {
            dirty.get(i).flushedSeconds = snapshots.get(i);
        }
    }

    /**
     * {@link #flushDirtyBuckets} wrapper for the flush schedule: swallows and logs
     * exceptions so the schedule never breaks.
     */
    private void flushDirtyBucketsQuietly() {
        try {
            flushDirtyBuckets();
        } catch (Exception e) {
            // Never break the flush schedule on database errors.
            logger.error("Error when flushing aggregation buckets", e);
        }
    }

    /**
     * Build the persisted snapshot of one bucket. The QPS fields carry the request TOTALS
     * of the seconds folded so far, i.e. "the QPS inside the N-second window" as displayed
     * on the page: with N=1000 and 3 requests the row shows pass_qps=3 (not a per-second
     * average). {@code rt} keeps the total-RT semantics (avg RT * successQps), so the
     * average RT computed on read as {@code rt / successQps} is the request-weighted mean
     * and stays correct for any N. The in-memory accumulator keeps its running totals for
     * further folding.
     */
    private MetricEntity normalizeBucket(BucketAccumulator accumulator) {
        MetricEntity bucket = accumulator.entity;
        MetricEntity normalized = new MetricEntity();
        normalized.setApp(bucket.getApp());
        normalized.setResource(bucket.getResource());
        normalized.setTimestamp(bucket.getTimestamp());
        normalized.setGmtCreate(bucket.getGmtCreate());
        normalized.setGmtModified(new Date());
        // N 秒窗口总量语义: 不做每秒平均归一化, 窗口内 3 次请求就存 3
        normalized.setPassQps(bucket.getPassQps());
        normalized.setSuccessQps(bucket.getSuccessQps());
        normalized.setBlockQps(bucket.getBlockQps());
        normalized.setExceptionQps(bucket.getExceptionQps());
        // rt 保持总RT(平均RT*成功QPS): 读取侧 rt/successQps 即请求加权平均响应时间
        normalized.setRt(bucket.getRt());
        normalized.setCount(bucket.getCount());
        return normalized;
    }

    /**
     * Batch upserts the given entities with {@link #UPSERT_SQL}; database errors are
     * caught and logged so the metric fetching loop is never broken.
     */
    private void doUpsert(List<MetricEntity> validMetrics) {
        try {
            jdbcTemplate.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MetricEntity metric = validMetrics.get(i);
                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    ps.setTimestamp(1, metric.getGmtCreate() == null ? now
                        : new Timestamp(metric.getGmtCreate().getTime()));
                    ps.setTimestamp(2, now);
                    ps.setString(3, metric.getApp());
                    ps.setLong(4, metric.getTimestamp().getTime());
                    ps.setString(5, metric.getResource());
                    ps.setLong(6, metric.getPassQps() == null ? 0 : metric.getPassQps());
                    ps.setLong(7, metric.getSuccessQps() == null ? 0 : metric.getSuccessQps());
                    ps.setLong(8, metric.getBlockQps() == null ? 0 : metric.getBlockQps());
                    ps.setLong(9, metric.getExceptionQps() == null ? 0 : metric.getExceptionQps());
                    ps.setDouble(10, metric.getRt());
                    ps.setInt(11, metric.getCount());
                }

                @Override
                public int getBatchSize() {
                    return validMetrics.size();
                }
            });
        } catch (Exception e) {
            // Never break the metric fetching loop on database errors.
            logger.error("Error when saving metrics to MySQL, size={}", validMetrics.size(), e);
        }
    }

    /**
     * Loads the metrics of one app+resource with timestamp in [startTime, endTime] (both
     * inclusive), ordered by timestamp ascending; returns an empty list for blank input
     * or on database error.
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
     * Lists the resources of the app that have metrics within [startTime, endTime], ordered
     * by summed block QPS then summed pass QPS descending; returns an empty list for a
     * blank app or on database error.
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
     * Deletes metric rows older than {@link #retentionDays} days in batches of
     * {@link #MAX_BATCH_DELETE_ROWS} until fewer rows than the batch limit are affected.
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
