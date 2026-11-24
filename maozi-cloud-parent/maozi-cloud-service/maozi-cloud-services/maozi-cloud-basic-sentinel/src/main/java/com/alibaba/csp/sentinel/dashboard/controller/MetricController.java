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
import com.alibaba.csp.sentinel.dashboard.domain.vo.MetricVO;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 为监控页面提供监控指标查询的控制器。
 *
 * @author leyou
 * @date 2026/11/22
 */
@Controller
@RequestMapping(value = "/metric", produces = MediaType.APPLICATION_JSON_VALUE)
public class MetricController {

    /** 日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(MetricController.class);

    /** 调用方未提供起始时间时的默认查询窗口（毫秒）：最近 5 分钟。 */
    private static final long DEFAULT_QUERY_INTERVAL_MS = 1000 * 60 * 5;

    /** 热点资源指标查询的默认每页数量。 */
    private static final int DEFAULT_PAGE_SIZE = 6;

    /** 热点资源指标查询的最大每页数量。 */
    private static final int MAX_PAGE_SIZE = 20;

    /** 每秒毫秒数，用于换算聚合桶的时长。 */
    private static final long MILLIS_PER_SECOND = 1000L;

    /** 热点资源查询响应的键数量（分页信息、聚合粒度与指标）。 */
    private static final int RESULT_ENTRY_COUNT = 6;

    /** 存储与查询指标实体的仓储。 */
    private final MetricsRepository<MetricEntity> metricStore;

    /**
     * 指标聚合粒度 N（秒），与 {@code JdbcMetricsRepository} 使用同一配置，
     * 用于对齐查询边界。
     */
    @Value("${sentinel.metric.aggregate-seconds:1}")
    private int aggregateSeconds;

    /** 注入指标仓储。 */
    public MetricController(MetricsRepository<MetricEntity> metricStore) {
        this.metricStore = metricStore;
    }

    /**
     * 查询应用在时间范围内的热点资源（仅返回在范围内有数据点的资源），
     * 可按 searchKey 过滤并分页；当前页每个资源附带其排序去重后的指标数据。
     */
    @ResponseBody
    @GetMapping("/queryTopResourceMetric.json")
    public Result<?> queryTopResourceMetric(final String app, Integer pageIndex, Integer pageSize,
            Boolean desc, Long startTime, Long endTime, String searchKey) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }
        if (pageIndex == null || pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize == null) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize >= MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
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
        long alignedStartTime = alignToBucket(startTime);
        long alignedEndTime = alignToBucketEnd(endTime);
        // 严格按查询窗口列出资源：窗口内无数据点的资源不展示
        List<String> resources = metricStore.listResourcesOfApp(app, alignedStartTime, alignedEndTime);
        // 缓存资源总数，调试日志与分页计算共用，避免同一方法内重复调用
        int totalCount = resources.size();
        logger.debug("queryTopResourceMetric(), resources.size()={}", totalCount);
        if (resources.isEmpty()) {
            return Result.ofSuccess(null);
        }
        if (!desc) {
            Collections.reverse(resources);
        }
        if (StringUtil.isNotEmpty(searchKey)) {
            resources = filterResources(resources, searchKey);
            // 按搜索关键字过滤后资源总数随之变化，同步刷新缓存值
            totalCount = resources.size();
        }
        int totalPage = (totalCount + pageSize - 1) / pageSize;
        List<String> topResources = pageResources(resources, pageIndex, pageSize, totalCount, totalPage);
        logger.debug("topResource={}", topResources);
        Map<String, Iterable<MetricVO>> metrics = queryResourceMetrics(app, topResources, alignedStartTime,
                alignedEndTime);
        return Result.ofSuccess(buildResultMap(totalCount, totalPage, pageIndex, pageSize, metrics));
    }

    /**
     * 查询应用指定资源在时间范围内的指标数据，按时间戳排序并去重。
     */
    @ResponseBody
    @GetMapping("/queryByAppAndResource.json")
    public Result<?> queryByAppAndResource(String app, String identity, Long startTime, Long endTime) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(identity)) {
            return Result.ofFail(Result.CODE_ERROR, "identity can't be null or empty");
        }
        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }
        if (startTime == null) {
            startTime = endTime - DEFAULT_QUERY_INTERVAL_MS;
        }
        List<MetricEntity> entities = metricStore.queryByAppAndResourceBetween(
                app, identity, alignToBucket(startTime), alignToBucketEnd(endTime));
        List<MetricVO> metricVos = MetricVO.fromMetricEntities(entities, identity);
        return Result.ofSuccess(sortMetricVoAndDistinct(metricVos));
    }

    /** 返回名称包含搜索关键字的资源，保持原有顺序。 */
    private List<String> filterResources(List<String> resources, String searchKey) {
        List<String> matched = new ArrayList<>();
        for (String resource : resources) {
            if (resource.contains(searchKey)) {
                matched.add(resource);
            }
        }
        return matched;
    }

    /** 截取指定页的资源；页码超出总页数时返回空列表。 */
    private List<String> pageResources(List<String> resources, int pageIndex, int pageSize, int totalCount,
            int totalPage) {
        if (pageIndex > totalPage) {
            return new ArrayList<>();
        }
        return resources.subList((pageIndex - 1) * pageSize, Math.min(pageIndex * pageSize, totalCount));
    }

    /** 查询各资源在时间范围内的指标并排序去重，按给定资源顺序保存。 */
    private Map<String, Iterable<MetricVO>> queryResourceMetrics(String app, List<String> resources,
            long startTime, long endTime) {
        Map<String, Iterable<MetricVO>> metrics = new LinkedHashMap<>();
        long beginTime = System.currentTimeMillis();
        for (String resource : resources) {
            List<MetricEntity> entities = metricStore.queryByAppAndResourceBetween(app, resource, startTime, endTime);
            logger.debug("resource={}, entities.size()={}", resource, entities == null ? "null" : entities.size());
            List<MetricVO> metricVos = MetricVO.fromMetricEntities(entities, resource);
            metrics.put(resource, sortMetricVoAndDistinct(metricVos));
        }
        logger.debug("queryResourceMetrics() total query time={} ms", System.currentTimeMillis() - beginTime);
        return metrics;
    }

    /** 组装热点资源查询的响应：分页信息、下发前端的聚合粒度与各资源指标。 */
    private Map<String, Object> buildResultMap(int totalCount, int totalPage, Integer pageIndex, Integer pageSize,
            Map<String, Iterable<MetricVO>> metrics) {
        Map<String, Object> resultMap = new HashMap<>(RESULT_ENTRY_COUNT);
        // 将聚合粒度 N 下发给前端：图表首尾补零依赖该值（数据点前后各补 N 秒）
        resultMap.put("aggregateSeconds", effectiveAggregateSeconds());
        resultMap.put("totalCount", totalCount);
        resultMap.put("totalPage", totalPage);
        resultMap.put("pageIndex", pageIndex);
        resultMap.put("pageSize", pageSize);
        resultMap.put("metric", metrics);
        return resultMap;
    }

    /** 生效的聚合粒度（秒）：配置为非正数时按 1 秒处理。 */
    private int effectiveAggregateSeconds() {
        return Math.max(1, aggregateSeconds);
    }

    /**
     * 将查询起始时间向下对齐到 N 秒桶边界：持久化行记录的是桶的起始时间戳，
     * 向下对齐既能看到正在采集中的桶，又不会引入更早的桶。
     */
    private long alignToBucket(long startTime) {
        long bucketMs = effectiveAggregateSeconds() * MILLIS_PER_SECOND;
        return startTime / bucketMs * bucketMs;
    }

    /**
     * 将查询结束时间向上对齐到 N 秒桶边界，与 {@link #alignToBucket(long)} 对称：
     * 窗口结束于桶中间时也能看到下一个桶，使两端均落在完整桶上。
     */
    private long alignToBucketEnd(long endTime) {
        long bucketMs = effectiveAggregateSeconds() * MILLIS_PER_SECOND;
        if (endTime % bucketMs == 0) {
            return endTime;
        }
        return (endTime / bucketMs + 1) * bucketMs;
    }

    /** 按时间戳升序排序 VO，重复时间戳仅保留创建时间最近的一条。 */
    private Iterable<MetricVO> sortMetricVoAndDistinct(List<MetricVO> metricVos) {
        if (metricVos == null) {
            return null;
        }
        Map<Long, MetricVO> metricByTimestamp = new TreeMap<>();
        for (MetricVO metricVo : metricVos) {
            // 缓存时间戳，避免同一方法内重复调用
            Long timestamp = metricVo.getTimestamp();
            MetricVO existing = metricByTimestamp.get(timestamp);
            if (existing == null || metricVo.getGmtCreate() > existing.getGmtCreate()) {
                metricByTimestamp.put(timestamp, metricVo);
            }
        }
        return metricByTimestamp.values();
    }
}
