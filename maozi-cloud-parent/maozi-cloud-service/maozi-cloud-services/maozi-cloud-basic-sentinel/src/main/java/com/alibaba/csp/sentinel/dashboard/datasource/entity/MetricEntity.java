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
 * 资源在某个时间片内一条监控指标记录的实体。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class MetricEntity extends BaseEntity {
    /** 指标时间戳。 */
    private Date timestamp;
    /** 资源名。 */
    private String resource;
    /** 通过 QPS。 */
    private Long passQps;
    /** 成功 QPS。 */
    private Long successQps;
    /** 阻塞 QPS。 */
    private Long blockQps;
    /** 异常 QPS。 */
    private Long exceptionQps;

    /** 全部成功请求的总 RT。 */
    private double rt;

    /** 本次聚合的总条数。 */
    private int count;

    /** 资源名的哈希码，设置资源名时同步更新。 */
    private int resourceCode;

    /** 复制指定实体的全部字段构造副本。 */
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

    /** 累加通过 QPS。 */
    public synchronized void addPassQps(Long passQps) {
        this.passQps += passQps;
    }

    /** 累加阻塞 QPS。 */
    public synchronized void addBlockQps(Long blockQps) {
        this.blockQps += blockQps;
    }

    /** 累加异常 QPS。 */
    public synchronized void addExceptionQps(Long exceptionQps) {
        this.exceptionQps += exceptionQps;
    }

    /** 累加聚合总条数。 */
    public synchronized void addCount(int count) {
        this.count += count;
    }

    /** 按 {@code avgRt * successQps} 累加总 RT，并累加成功 QPS。 */
    public synchronized void addRtAndSuccessQps(double avgRt, Long successQps) {
        this.rt += avgRt * successQps;
        this.successQps += successQps;
    }

    /**
     * 将 {@link #rt} 设置为 {@code avgRt * successQps}，成功 QPS 设置为指定值。
     *
     * @param avgRt      successQps 个请求的平均 RT
     * @param successQps 成功 QPS
     */
    public synchronized void setRtAndSuccessQps(double avgRt, Long successQps) {
        this.rt = avgRt * successQps;
        this.successQps = successQps;
    }

    /** 获取指标时间戳。 */
    public Date getTimestamp() {
        return timestamp;
    }

    /** 设置指标时间戳。 */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /** 获取资源名。 */
    public String getResource() {
        return resource;
    }

    /** 设置资源名，并将其哈希码同步更新到 resourceCode。 */
    public void setResource(String resource) {
        this.resource = resource;
        this.resourceCode = resource.hashCode();
    }

    /** 获取通过 QPS。 */
    public Long getPassQps() {
        return passQps;
    }

    /** 设置通过 QPS。 */
    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    /** 获取阻塞 QPS。 */
    public Long getBlockQps() {
        return blockQps;
    }

    /** 设置阻塞 QPS。 */
    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    /** 获取异常 QPS。 */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** 设置异常 QPS。 */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** 获取全部成功请求的总 RT。 */
    public double getRt() {
        return rt;
    }

    /** 设置全部成功请求的总 RT。 */
    public void setRt(double rt) {
        this.rt = rt;
    }

    /** 获取本次聚合的总条数。 */
    public int getCount() {
        return count;
    }

    /** 设置本次聚合的总条数。 */
    public void setCount(int count) {
        this.count = count;
    }

    /** 获取资源名的哈希码。 */
    public int getResourceCode() {
        return resourceCode;
    }

    /** 获取成功 QPS。 */
    public Long getSuccessQps() {
        return successQps;
    }

    /** 设置成功 QPS。 */
    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    /** 拼接全部字段构建字符串。 */
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
