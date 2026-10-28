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
package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.vo.MetricVo;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leyou
 */
@Controller
@RequestMapping(value = "/metric", produces = MediaType.APPLICATION_JSON_VALUE)
public class MetricController {

    /** The logger of this controller. */
    private static Logger logger = LoggerFactory.getLogger(MetricController.class);

    /**
     * Default query window when the caller does not provide a start time: the last 5 minutes.
     */
    private static final long DEFAULT_QUERY_INTERVAL_MS = 1000 * 60 * 5;

    /** The repository that stores and queries metric entities. */
    @Autowired
    private MetricsRepository<MetricEntity> metricStore;

    /**
     * Same configuration as {@code JdbcMetricsRepository}: the metric aggregation
     * granularity N in seconds, used to align query time boundaries to the bucket grid.
     */
    @Value("${sentinel.metric.aggregate-seconds:1}")
    private int aggregateSeconds;

    /**
     * Queries the top resources of the app within the time range (only those with data points
     * in range), optionally filtered by searchKey and paginated; each resource on the current
     * page is returned with its sorted, de-duplicated metric data.
     */
    @ResponseBody
    @RequestMapping("/queryTopResourceMetric.json")
    public Result<?> queryTopResourceMetric(final String app,
                                            Integer pageIndex,
                                            Integer pageSize,
                                            Boolean desc,
                                            Long startTime, Long endTime, String searchKey) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (pageIndex == null || pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize == null) {
            pageSize = 6;
        }
        if (pageSize >= 20) {
            pageSize = 20;
        }
        if (desc == null) {
            desc = true;
        }
        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }
        if (startTime == null) {
            startTime = endTime - DEFAULT_QUERY_INTERVAL_MS;
        }
        startTime = alignToBucket(startTime);
        endTime = alignToBucketEnd(endTime);
        // 严格按查询区间列出资源: 区间内没有数据点的资源不展示
        List<String> resources = metricStore.listResourcesOfApp(app, startTime, endTime);
        logger.debug("queryTopResourceMetric(), resources.size()={}", resources.size());

        if (resources == null || resources.isEmpty()) {
            return Result.ofSuccess(null);
        }
        if (!desc) {
            Collections.reverse(resources);
        }
        if (StringUtil.isNotEmpty(searchKey)) {
            List<String> searched = new ArrayList<>();
            for (String resource : resources) {
                if (resource.contains(searchKey)) {
                    searched.add(resource);
                }
            }
            resources = searched;
        }
        int totalPage = (resources.size() + pageSize - 1) / pageSize;
        List<String> topResource = new ArrayList<>();
        if (pageIndex <= totalPage) {
            topResource = resources.subList((pageIndex - 1) * pageSize,
                Math.min(pageIndex * pageSize, resources.size()));
        }
        final Map<String, Iterable<MetricVo>> map = new ConcurrentHashMap<>();
        logger.debug("topResource={}", topResource);
        long time = System.currentTimeMillis();
        for (final String resource : topResource) {
            List<MetricEntity> entities = metricStore.queryByAppAndResourceBetween(
                app, resource, startTime, endTime);
            logger.debug("resource={}, entities.size()={}", resource, entities == null ? "null" : entities.size());
            List<MetricVo> vos = MetricVo.fromMetricEntities(entities, resource);
            Iterable<MetricVo> vosSorted = sortMetricVoAndDistinct(vos);
            map.put(resource, vosSorted);
        }
        logger.debug("queryTopResourceMetric() total query time={} ms", System.currentTimeMillis() - time);
        Map<String, Object> resultMap = new HashMap<>(16);
        // 聚合粒度 N 下发给前端: 图表首尾补零点的位置依赖它(点前/后 N 秒)
        resultMap.put("aggregateSeconds", Math.max(1, aggregateSeconds));
        resultMap.put("totalCount", resources.size());
        resultMap.put("totalPage", totalPage);
        resultMap.put("pageIndex", pageIndex);
        resultMap.put("pageSize", pageSize);

        Map<String, Iterable<MetricVo>> map2 = new LinkedHashMap<>();
        // order matters.
        for (String identity : topResource) {
            map2.put(identity, map.get(identity));
        }
        resultMap.put("metric", map2);
        return Result.ofSuccess(resultMap);
    }

    /** Queries the metric data of the given resource of the app in the time range, sorted and de-duplicated by timestamp. */
    @ResponseBody
    @RequestMapping("/queryByAppAndResource.json")
    public Result<?> queryByAppAndResource(String app, String identity, Long startTime, Long endTime) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(identity)) {
            return Result.ofFail(-1, "identity can't be null or empty");
        }
        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }
        if (startTime == null) {
            startTime = endTime - DEFAULT_QUERY_INTERVAL_MS;
        }
        List<MetricEntity> entities = metricStore.queryByAppAndResourceBetween(
            app, identity, alignToBucket(startTime), alignToBucketEnd(endTime));
        List<MetricVo> vos = MetricVo.fromMetricEntities(entities, identity);
        return Result.ofSuccess(sortMetricVoAndDistinct(vos));
    }

    /**
     * Align the query start time down to the N-second bucket grid. With aggregation the
     * persisted row of a bucket carries the bucket START timestamp, so the bucket that is
     * currently collecting (whose start may be up to N-1 seconds before {@code startTime})
     * would otherwise never match a short query window such as "the last 5 minutes" and the
     * page would show nothing although the data is already in MySQL. Aligning down makes
     * every bucket INTERSECTING the window visible, without pulling in older buckets.
     */
    private long alignToBucket(long startTime) {
        long bucketMs = Math.max(1, aggregateSeconds) * 1000L;
        return startTime / bucketMs * bucketMs;
    }

    /**
     * Align the query end time UP to the N-second bucket grid, symmetric to
     * {@link #alignToBucket(long)}: a window ending mid-bucket also makes the bucket
     * starting right after {@code endTime} visible (e.g. 05:05:25 -> 05:05:30 with N=10),
     * so both edges of the window resolve to complete buckets.
     */
    private long alignToBucketEnd(long endTime) {
        long bucketMs = Math.max(1, aggregateSeconds) * 1000L;
        if (endTime % bucketMs == 0) {
            return endTime;
        }
        return (endTime / bucketMs + 1) * bucketMs;
    }

    /** Sorts the VOs by timestamp ascending, keeping only the most recently created VO per duplicated timestamp. */
    private Iterable<MetricVo> sortMetricVoAndDistinct(List<MetricVo> vos) {
        if (vos == null) {
            return null;
        }
        Map<Long, MetricVo> map = new TreeMap<>();
        for (MetricVo vo : vos) {
            MetricVo oldVo = map.get(vo.getTimestamp());
            if (oldVo == null || vo.getGmtCreate() > oldVo.getGmtCreate()) {
                map.put(vo.getTimestamp(), vo);
            }
        }
        return map.values();
    }
}
