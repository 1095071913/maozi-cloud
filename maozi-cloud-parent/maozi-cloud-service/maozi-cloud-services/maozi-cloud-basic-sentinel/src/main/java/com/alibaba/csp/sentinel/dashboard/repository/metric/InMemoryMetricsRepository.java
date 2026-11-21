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
import com.alibaba.csp.sentinel.util.TimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 将一段时间内的指标数据缓存在内存中
 * <p>
 * 默认 {@code @Component} 已由 {@link JdbcMetricsRepository} 取代，
 * 保留该实现供参考与单元测试使用
 *
 * @author Carpenter Lee
 * @author Eric Zhao
 * @date 2026/11/22
 */
public class InMemoryMetricsRepository implements MetricsRepository<MetricEntity> {

    /** 指标实体在内存缓存中的最长保留时间（毫秒） */
    private static final long MAX_METRIC_LIVE_TIME_MS = 1000 * 60 * 5;

    /** 全部缓存指标，结构为 {@code app -> resource -> timestamp -> metric} */
    private Map<String, Map<String, LinkedHashMap<Long, MetricEntity>>> allMetrics = new ConcurrentHashMap<>();

    /** 保护 {@link #allMetrics} 访问的读写锁 */
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();


    /**
     * 按应用/资源/时间戳保存单个指标实体，入参为 null 或应用名为空时忽略，
     * 更新 Map 期间持有写锁
     */
    @Override
    public void save(MetricEntity entity) {
        if (entity == null || StringUtil.isBlank(entity.getApp())) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            allMetrics.computeIfAbsent(entity.getApp(), e -> new HashMap<>(16))
                    .computeIfAbsent(entity.getResource(), e -> new LinkedHashMap<Long, MetricEntity>() {
                        /** 每次插入时淘汰超过 {@link #MAX_METRIC_LIVE_TIME_MS} 的旧指标 */
                        @Override
                        protected boolean removeEldestEntry(Entry<Long, MetricEntity> eldest) {
                            return eldest.getKey() < TimeUtil.currentTimeMillis() - MAX_METRIC_LIVE_TIME_MS;
                        }
                    }).put(entity.getTimestamp().getTime(), entity);
        } finally {
            readWriteLock.writeLock().unlock();
        }

    }

    /**
     * 逐条调用 {@link #save} 保存全部指标实体，整个批处理期间持有写锁，
     * 入参为 null 时忽略
     */
    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            metrics.forEach(this::save);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * 查询指定应用和资源在 [startTime, endTime] 区间（双边均含）内的缓存指标，
     * 应用名为空或应用/资源不存在时返回空列表
     */
    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource,
                                                           long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        Map<String, LinkedHashMap<Long, MetricEntity>> resourceMap = allMetrics.get(app);
        if (resourceMap == null) {
            return results;
        }
        LinkedHashMap<Long, MetricEntity> metricsMap = resourceMap.get(resource);
        if (metricsMap == null) {
            return results;
        }
        readWriteLock.readLock().lock();
        try {
            for (Entry<Long, MetricEntity> entry : metricsMap.entrySet()) {
                long timestamp = entry.getKey();
                if (timestamp >= startTime && timestamp <= endTime) {
                    results.add(entry.getValue());
                }
            }
            return results;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * 列出指定应用在 [startTime, endTime] 区间内有指标数据的资源，
     * 指标按查询窗口聚合后按阻塞 QPS 降序排列（相同时按通过 QPS 降序），
     * 应用名为空或不存在时返回空列表
     */
    @Override
    public List<String> listResourcesOfApp(String app, long startTime, long endTime) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        // resource -> timestamp -> metric
        Map<String, LinkedHashMap<Long, MetricEntity>> resourceMap = allMetrics.get(app);
        if (resourceMap == null) {
            return results;
        }
        Map<String, MetricEntity> resourceCount = new ConcurrentHashMap<>(32);

        readWriteLock.readLock().lock();
        try {
            for (Entry<String, LinkedHashMap<Long, MetricEntity>> resourceMetrics : resourceMap.entrySet()) {
                for (Entry<Long, MetricEntity> metrics : resourceMetrics.getValue().entrySet()) {
                    long metricTime = metrics.getKey();
                    if (metricTime < startTime || metricTime > endTime) {
                        continue;
                    }
                    MetricEntity newEntity = metrics.getValue();
                    MetricEntity oldEntity = resourceCount.get(resourceMetrics.getKey());
                    if (oldEntity != null) {
                        oldEntity.addPassQps(newEntity.getPassQps());
                        oldEntity.addRtAndSuccessQps(newEntity.getRt(), newEntity.getSuccessQps());
                        oldEntity.addBlockQps(newEntity.getBlockQps());
                        oldEntity.addExceptionQps(newEntity.getExceptionQps());
                        oldEntity.addCount(1);
                    } else {
                        resourceCount.put(resourceMetrics.getKey(), MetricEntity.copyOf(newEntity));
                    }
                }
            }
            // 按查询窗口内的阻塞 QPS 降序排列
            return resourceCount.entrySet()
                    .stream()
                    .sorted((o1, o2) -> {
                        MetricEntity e1 = o1.getValue();
                        MetricEntity e2 = o2.getValue();
                        int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                        if (t != 0) {
                            return t;
                        }
                        return e2.getPassQps().compareTo(e1.getPassQps());
                    })
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
}
