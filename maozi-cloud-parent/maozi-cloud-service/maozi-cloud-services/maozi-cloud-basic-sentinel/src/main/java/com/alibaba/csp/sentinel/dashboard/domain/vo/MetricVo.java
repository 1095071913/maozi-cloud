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

/**
 * @author leyou
 */
public class MetricVo implements Comparable<MetricVo> {
    /** The ID of the metric record. */
    private Long id;

    /** The app name of the metric record. */
    private String app;

    /** The timestamp of the metric record in milliseconds. */
    private Long timestamp;

    /** The create time in milliseconds, initialized to the current time. */
    private Long gmtCreate = System.currentTimeMillis();

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
    /**
     * average rt
     */
    private Double rt;

    /** The total number of aggregated entries in this record. */
    private Integer count;

    /** Create an empty vo. */
    public MetricVo() {
    }

    /** Convert the entities to a list of VOs; a null collection yields an empty list. */
    public static List<MetricVo> fromMetricEntities(Collection<MetricEntity> entities) {
        List<MetricVo> list = new ArrayList<>();
        if (entities != null) {
            for (MetricEntity entity : entities) {
                list.add(fromMetricEntity(entity));
            }
        }
        return list;
    }

    /**
     * 保留资源名为identity的结果。
     *
     * @param entities 通过hashCode查找到的MetricEntities
     * @param identity 真正需要查找的资源名
     * @return
     */
    public static List<MetricVo> fromMetricEntities(Collection<MetricEntity> entities, String identity) {
        List<MetricVo> list = new ArrayList<>();
        if (entities != null) {
            for (MetricEntity entity : entities) {
                if (entity.getResource().equals(identity)) {
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
    public static MetricVo fromMetricEntity(MetricEntity entity) {
        MetricVo vo = new MetricVo();
        vo.id = entity.getId();
        vo.app = entity.getApp();
        vo.timestamp = entity.getTimestamp().getTime();
        vo.gmtCreate = entity.getGmtCreate().getTime();
        vo.resource = entity.getResource();
        vo.passQps = entity.getPassQps();
        vo.blockQps = entity.getBlockQps();
        vo.successQps = entity.getSuccessQps();
        vo.exceptionQps = entity.getExceptionQps();
        if (entity.getSuccessQps() != 0) {
            vo.rt = entity.getRt() / entity.getSuccessQps();
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
    public static MetricVo parse(String line) {
        String[] strs = line.split("\\|");
        long timestamp = Long.parseLong(strs[0]);
        String identity = strs[1];
        long passQps = Long.parseLong(strs[2]);
        long blockQps = Long.parseLong(strs[3]);
        long exception = Long.parseLong(strs[4]);
        double rt = Double.parseDouble(strs[5]);
        long successQps = Long.parseLong(strs[6]);
        MetricVo vo = new MetricVo();
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

    /** Get the average rt. */
    public Double getRt() {
        return rt;
    }

    /** Set the average rt. */
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
    public int compareTo(MetricVo o) {
        return Long.compare(this.timestamp, o.timestamp);
    }
}
