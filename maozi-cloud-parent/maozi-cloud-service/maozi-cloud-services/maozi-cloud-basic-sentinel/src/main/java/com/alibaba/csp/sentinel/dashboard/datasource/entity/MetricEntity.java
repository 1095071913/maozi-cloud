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
public class MetricEntity {
    /** The entity id. */
    private Long id;
    /** The create time. */
    private Date gmtCreate;
    /** The last modify time. */
    private Date gmtModified;
    /** The application name. */
    private String app;
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

    /** Create a copy of the given entity with all fields copied. */
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

    /** Add the given value to the passed QPS. */
    public synchronized void addPassQps(Long passQps) {
        this.passQps += passQps;
    }

    /** Add the given value to the blocked QPS. */
    public synchronized void addBlockQps(Long blockQps) {
        this.blockQps += blockQps;
    }

    /** Add the given value to the exception QPS. */
    public synchronized void addExceptionQps(Long exceptionQps) {
        this.exceptionQps += exceptionQps;
    }

    /** Add the given value to the total count. */
    public synchronized void addCount(int count) {
        this.count += count;
    }

    /**
     * Accumulate the total rt by {@code avgRt * successQps} and add the given success QPS.
     */
    public synchronized void addRtAndSuccessQps(double avgRt, Long successQps) {
        this.rt += avgRt * successQps;
        this.successQps += successQps;
    }

    /**
     * Set {@link #rt} to {@code avgRt * successQps} and the success QPS to the given value.
     *
     * @param avgRt      the average RT of the {@code successQps} requests
     * @param successQps the success QPS
     */
    public synchronized void setRtAndSuccessQps(double avgRt, Long successQps) {
        this.rt = avgRt * successQps;
        this.successQps = successQps;
    }

    /** Get the entity id. */
    public Long getId() {
        return id;
    }

    /** Set the entity id. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Get the create time. */
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /** Set the create time. */
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /** Get the last modify time. */
    public Date getGmtModified() {
        return gmtModified;
    }

    /** Set the last modify time. */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    /** Get the application name. */
    public String getApp() {
        return app;
    }

    /** Set the application name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the timestamp of the metric. */
    public Date getTimestamp() {
        return timestamp;
    }

    /** Set the timestamp of the metric. */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /** Get the resource name. */
    public String getResource() {
        return resource;
    }

    /** Set the resource name and update resourceCode to its hash code. */
    public void setResource(String resource) {
        this.resource = resource;
        this.resourceCode = resource.hashCode();
    }

    /** Get the passed QPS. */
    public Long getPassQps() {
        return passQps;
    }

    /** Set the passed QPS. */
    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    /** Get the blocked QPS. */
    public Long getBlockQps() {
        return blockQps;
    }

    /** Set the blocked QPS. */
    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    /** Get the exception QPS. */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** Set the exception QPS. */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** Get the summary RT of all success exits. */
    public double getRt() {
        return rt;
    }

    /** Set the summary RT of all success exits. */
    public void setRt(double rt) {
        this.rt = rt;
    }

    /** Get the total count of this aggregation. */
    public int getCount() {
        return count;
    }

    /** Set the total count of this aggregation. */
    public void setCount(int count) {
        this.count = count;
    }

    /** Get the hash code of the resource name. */
    public int getResourceCode() {
        return resourceCode;
    }

    /** Get the success QPS. */
    public Long getSuccessQps() {
        return successQps;
    }

    /** Set the success QPS. */
    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    /** Build a string with all fields of this entity. */
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
