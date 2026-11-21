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
 * 发送给前端的单条监控指标记录视图对象。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class MetricVO implements Comparable<MetricVO> {

    /** 预编译的指标行分隔符正则。 */
    private static final Pattern METRIC_LINE_PATTERN = Pattern.compile("\\|");

    /** 监控指标记录 id。 */
    private Long id;

    /** 监控指标记录所属应用名。 */
    private String app;

    /** 监控指标记录的时间戳，单位毫秒。 */
    private Long timestamp;

    /** 创建时间，单位毫秒。 */
    private Long gmtCreate;

    /** 资源名。 */
    private String resource;

    /** 通过请求的 QPS。 */
    private Long passQps;

    /** 拒绝请求的 QPS。 */
    private Long blockQps;

    /** 成功请求的 QPS。 */
    private Long successQps;

    /** 异常请求的 QPS。 */
    private Long exceptionQps;
    /** 平均响应时间。 */
    private Double rt;

    /** 该记录聚合的条目总数。 */
    private Integer count;

    /** 创建空的 VO。 */
    public MetricVO() {
    }

    /** 将实体集合转换为 VO 列表，集合为 null 时返回空列表。 */
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
     * 仅转换资源名与指定标识完全相等的实体。
     *
     * @param entities 按资源哈希查找到的监控实体集合
     * @param identity 需精确匹配的资源名
     * @return 匹配实体对应的 VO 列表
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
     * 将指定实体转换为 VO：VO 的 rt 为实体 rt 除以成功 QPS，成功 QPS 为 0 时取 0。
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
        // 缓存成功 QPS，避免同一方法内重复获取
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
     * 将格式为
     * {@code timestamp|resource|passQps|blockQps|exceptionQps|rt|successQps}
     * 的指标行解析为 VO，count 固定为 1。
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

    /** 获取监控指标记录 id。 */
    public Long getId() {
        return id;
    }

    /** 设置监控指标记录 id。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取应用名。 */
    public String getApp() {
        return app;
    }

    /** 设置应用名。 */
    public void setApp(String app) {
        this.app = app;
    }

    /** 获取时间戳，单位毫秒。 */
    public Long getTimestamp() {
        return timestamp;
    }

    /** 设置时间戳，单位毫秒。 */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /** 获取创建时间，单位毫秒。 */
    public Long getGmtCreate() {
        return gmtCreate;
    }

    /** 设置创建时间，单位毫秒。 */
    public void setGmtCreate(Long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /** 获取资源名。 */
    public String getResource() {
        return resource;
    }

    /** 设置资源名。 */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** 获取通过请求的 QPS。 */
    public Long getPassQps() {
        return passQps;
    }

    /** 设置通过请求的 QPS。 */
    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    /** 获取拒绝请求的 QPS。 */
    public Long getBlockQps() {
        return blockQps;
    }

    /** 设置拒绝请求的 QPS。 */
    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    /** 获取成功请求的 QPS。 */
    public Long getSuccessQps() {
        return successQps;
    }

    /** 设置成功请求的 QPS。 */
    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    /** 获取异常请求的 QPS。 */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** 设置异常请求的 QPS。 */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** 获取平均响应时间。 */
    public Double getRt() {
        return rt;
    }

    /** 设置平均响应时间。 */
    public void setRt(Double rt) {
        this.rt = rt;
    }

    /** 获取聚合的条目总数。 */
    public Integer getCount() {
        return count;
    }

    /** 设置聚合的条目总数。 */
    public void setCount(Integer count) {
        this.count = count;
    }

    /** 按时间戳升序比较。 */
    @Override
    public int compareTo(MetricVO o) {
        return Long.compare(this.timestamp, o.timestamp);
    }

    /** 返回该 VO 的字符串表示。 */
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
