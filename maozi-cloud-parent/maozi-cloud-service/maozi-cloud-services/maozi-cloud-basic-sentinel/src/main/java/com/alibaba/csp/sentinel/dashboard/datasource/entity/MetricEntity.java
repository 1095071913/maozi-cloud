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
package com.alibaba.csp.sentinel.dashboard.datasource.entity;

import java.util.Date;

/**
 * Entity of one metric record of a resource in a time slot.
 *
 * @author leyou
 */
public class MetricEntity extends BaseEntity {
    /** The timestamp of the metric. */
    private Date timestamp;
    /** The resource name. */
    private String resource;
    /** The passed QPS. */
    private Long passQps;
    /** The success QPS. */
    private Long successQps;
    /** The blocked QPS. */
    private Long blockQps;
    /** The exception QPS. */
    private Long exceptionQps;

    /** The summary RT of all success exits. */
    private double rt;

    /** The total count of this aggregation. */
    private int count;

    /** The hash code of the resource name, updated when the resource is set. */
    private int resourceCode;

    /** Creates a copy of the given entity with all fields copied. */
    public static MetricEntity copyOf(MetricEntity oldEntity) {
        MetricEntity entity = new MetricEntity();
        entity.setId(oldEntity.getId());
        entity.setGmtCreate(oldEntity.getGmtCreate());
        entity.setGmtModified(oldEntity.getGmtModified());
        entity.setApp(oldEntity.getApp());
        entity.setTimestamp(oldEntity.getTimestamp());
        entity.setResource(oldEntity.getResource());
        entity.setPassQps(oldEntity.getPassQps());
        entity.setBlockQps(oldEntity.getBlockQps());
        entity.setSuccessQps(oldEntity.getSuccessQps());
        entity.setExceptionQps(oldEntity.getExceptionQps());
        entity.setRt(oldEntity.getRt());
        entity.setCount(oldEntity.getCount());
        return entity;
    }

    /** Adds the given value to the passed QPS. */
    public synchronized void addPassQps(Long passQps) {
        this.passQps += passQps;
    }

    /** Adds the given value to the blocked QPS. */
    public synchronized void addBlockQps(Long blockQps) {
        this.blockQps += blockQps;
    }

    /** Adds the given value to the exception QPS. */
    public synchronized void addExceptionQps(Long exceptionQps) {
        this.exceptionQps += exceptionQps;
    }

    /** Adds the given value to the total count. */
    public synchronized void addCount(int count) {
        this.count += count;
    }

    /**
     * Accumulates the total rt by {@code avgRt * successQps} and adds the given success QPS.
     */
    public synchronized void addRtAndSuccessQps(double avgRt, Long successQps) {
        this.rt += avgRt * successQps;
        this.successQps += successQps;
    }

    /**
     * Sets {@link #rt} to {@code avgRt * successQps} and the success QPS to the given value.
     *
     * @param avgRt      the average RT of the {@code successQps} requests
     * @param successQps the success QPS
     */
    public synchronized void setRtAndSuccessQps(double avgRt, Long successQps) {
        this.rt = avgRt * successQps;
        this.successQps = successQps;
    }

    /** Gets the timestamp of the metric. */
    public Date getTimestamp() {
        return timestamp;
    }

    /** Sets the timestamp of the metric. */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /** Gets the resource name. */
    public String getResource() {
        return resource;
    }

    /** Sets the resource name and updates resourceCode to its hash code. */
    public void setResource(String resource) {
        this.resource = resource;
        this.resourceCode = resource.hashCode();
    }

    /** Gets the passed QPS. */
    public Long getPassQps() {
        return passQps;
    }

    /** Sets the passed QPS. */
    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    /** Gets the blocked QPS. */
    public Long getBlockQps() {
        return blockQps;
    }

    /** Sets the blocked QPS. */
    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    /** Gets the exception QPS. */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** Sets the exception QPS. */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** Gets the summary RT of all success exits. */
    public double getRt() {
        return rt;
    }

    /** Sets the summary RT of all success exits. */
    public void setRt(double rt) {
        this.rt = rt;
    }

    /** Gets the total count of this aggregation. */
    public int getCount() {
        return count;
    }

    /** Sets the total count of this aggregation. */
    public void setCount(int count) {
        this.count = count;
    }

    /** Gets the hash code of the resource name. */
    public int getResourceCode() {
        return resourceCode;
    }

    /** Gets the success QPS. */
    public Long getSuccessQps() {
        return successQps;
    }

    /** Sets the success QPS. */
    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    /** Builds a string with all fields of this entity. */
    @Override
    public String toString() {
        return "MetricEntity{" +
            "id=" + id +
            ", gmtCreate=" + gmtCreate +
            ", gmtModified=" + gmtModified +
            ", app='" + app + '\'' +
            ", timestamp=" + timestamp +
            ", resource='" + resource + '\'' +
            ", passQps=" + passQps +
            ", blockQps=" + blockQps +
            ", successQps=" + successQps +
            ", exceptionQps=" + exceptionQps +
            ", rt=" + rt +
            ", count=" + count +
            ", resourceCode=" + resourceCode +
            '}';
    }

}
