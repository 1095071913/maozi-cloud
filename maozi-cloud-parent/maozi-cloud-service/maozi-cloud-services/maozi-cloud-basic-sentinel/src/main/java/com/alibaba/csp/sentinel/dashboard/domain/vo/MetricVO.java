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
package com.alibaba.csp.sentinel.dashboard.domain.vo;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * View object of one metric record sent to the frontend.
 *
 * @author leyou
 * @date 2026/11/22
 */
public class MetricVO implements Comparable<MetricVO> {

    /** Precompiled pattern of the metric line separator. */
    private static final Pattern METRIC_LINE_PATTERN = Pattern.compile("\\|");

    /** The ID of the metric record. */
    private Long id;

    /** The app name of the metric record. */
    private String app;

    /** The timestamp of the metric record in milliseconds. */
    private Long timestamp;

    /** The create time in milliseconds. */
    private Long gmtCreate;

    /** The resource name. */
    private String resource;

    /** The QPS of passed requests. */
    private Long passQps;

    /** The QPS of blocked requests. */
    private Long blockQps;

    /** The QPS of successful requests. */
    private Long successQps;

    /** The QPS of exceptions. */
    private Long exceptionQps;
    /** The average response time. */
    private Double rt;

    /** The total number of aggregated entries in this record. */
    private Integer count;

    /** Create an empty vo. */
    public MetricVO() {
    }

    /** Convert the entities to a list of VOs; a null collection yields an empty list. */
    public static List<MetricVO> fromMetricEntities(Collection<MetricEntity> entities) {
        List<MetricVO> list = new ArrayList<>(entities == null ? 0 : entities.size());
        if (entities != null) {
            for (MetricEntity entity : entities) {
                list.add(fromMetricEntity(entity));
            }
        }
        return list;
    }

    /**
     * Convert only the entities whose resource name equals the given identity.
     *
     * @param entities metric entities found by resource hash code
     * @param identity the exact resource name to match
     * @return VOs of the matching entities
     */
    public static List<MetricVO> fromMetricEntities(Collection<MetricEntity> entities, String identity) {
        List<MetricVO> list = new ArrayList<>(entities == null ? 0 : entities.size());
        if (entities != null) {
            for (MetricEntity entity : entities) {
                if (identity.equals(entity.getResource())) {
                    list.add(fromMetricEntity(entity));
                }
            }
        }
        return list;
    }

    /**
     * Convert the given entity to a VO. The rt of the VO is the entity rt divided
     * by the success QPS, or 0 if the success QPS is 0.
     */
    public static MetricVO fromMetricEntity(MetricEntity entity) {
        MetricVO vo = new MetricVO();
        vo.id = entity.getId();
        vo.app = entity.getApp();
        vo.timestamp = entity.getTimestamp().getTime();
        vo.gmtCreate = entity.getGmtCreate().getTime();
        vo.resource = entity.getResource();
        vo.passQps = entity.getPassQps();
        vo.blockQps = entity.getBlockQps();
        // Cache the success QPS to avoid repeated calls within the same method.
        Long successQps = entity.getSuccessQps();
        vo.successQps = successQps;
        vo.exceptionQps = entity.getExceptionQps();
        if (successQps != null && successQps != 0) {
            vo.rt = entity.getRt() / successQps;
        } else {
            vo.rt = 0D;
        }
        vo.count = entity.getCount();
        return vo;
    }

    /**
     * Parse a metric line in the format
     * {@code timestamp|resource|passQps|blockQps|exceptionQps|rt|successQps}
     * into a VO, with the count set to 1.
     */
    public static MetricVO parse(String line) {
        String[] strs = METRIC_LINE_PATTERN.split(line);
        long timestamp = Long.parseLong(strs[0]);
        String identity = strs[1];
        long passQps = Long.parseLong(strs[2]);
        long blockQps = Long.parseLong(strs[3]);
        long exception = Long.parseLong(strs[4]);
        double rt = Double.parseDouble(strs[5]);
        long successQps = Long.parseLong(strs[6]);
        MetricVO vo = new MetricVO();
        vo.timestamp = timestamp;
        vo.resource = identity;
        vo.passQps = passQps;
        vo.blockQps = blockQps;
        vo.successQps = successQps;
        vo.exceptionQps = exception;
        vo.rt = rt;
        vo.count = 1;
        return vo;
    }

    /** Get the ID of the metric record. */
    public Long getId() {
        return id;
    }

    /** Set the ID of the metric record. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Get the app name. */
    public String getApp() {
        return app;
    }

    /** Set the app name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the timestamp in milliseconds. */
    public Long getTimestamp() {
        return timestamp;
    }

    /** Set the timestamp in milliseconds. */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /** Get the create time in milliseconds. */
    public Long getGmtCreate() {
        return gmtCreate;
    }

    /** Set the create time in milliseconds. */
    public void setGmtCreate(Long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /** Get the resource name. */
    public String getResource() {
        return resource;
    }

    /** Set the resource name. */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Get the QPS of passed requests. */
    public Long getPassQps() {
        return passQps;
    }

    /** Set the QPS of passed requests. */
    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    /** Get the QPS of blocked requests. */
    public Long getBlockQps() {
        return blockQps;
    }

    /** Set the QPS of blocked requests. */
    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    /** Get the QPS of successful requests. */
    public Long getSuccessQps() {
        return successQps;
    }

    /** Set the QPS of successful requests. */
    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    /** Get the QPS of exceptions. */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** Set the QPS of exceptions. */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** Get the average response time. */
    public Double getRt() {
        return rt;
    }

    /** Set the average response time. */
    public void setRt(Double rt) {
        this.rt = rt;
    }

    /** Get the total number of aggregated entries. */
    public Integer getCount() {
        return count;
    }

    /** Set the total number of aggregated entries. */
    public void setCount(Integer count) {
        this.count = count;
    }

    /** Compare VOs by timestamp in ascending order. */
    @Override
    public int compareTo(MetricVO o) {
        return Long.compare(this.timestamp, o.timestamp);
    }

    /** Return a string representation of this VO. */
    @Override
    public String toString() {
        return "MetricVO{"
                + "id=" + id
                + ", app='" + app + '\''
                + ", timestamp=" + timestamp
                + ", gmtCreate=" + gmtCreate
                + ", resource='" + resource + '\''
                + ", passQps=" + passQps
                + ", blockQps=" + blockQps
                + ", successQps=" + successQps
                + ", exceptionQps=" + exceptionQps
                + ", rt=" + rt
                + ", count=" + count
                + '}';
    }
}
