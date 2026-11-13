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
package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

import com.alibaba.csp.sentinel.slots.system.SystemRule;

import java.util.Date;

/**
 * Entity for a system adaptive protection rule.
 *
 * @author leyou
 */
public class SystemRuleEntity implements RuleEntity {

    /** The entity id. */
    private Long id;

    /** The application name. */
    private String app;
    /** The IP address of the machine. */
    private String ip;
    /** The port of the machine. */
    private Integer port;
    /** The highest system load threshold. */
    private Double highestSystemLoad;
    /** The average RT (ms) threshold for all inbound requests. */
    private Long avgRt;
    /** The max concurrent thread count threshold. */
    private Long maxThread;
    /** The max total QPS of all inbound requests. */
    private Double qps;
    /** The highest CPU usage threshold. */
    private Double highestCpuUsage;

    /** The create time. */
    private Date gmtCreate;
    /** The last modify time. */
    private Date gmtModified;

    /**
     * Build an entity from the given {@link SystemRule}, carrying the given app, ip and port.
     */
    public static SystemRuleEntity fromSystemRule(String app, String ip, Integer port, SystemRule rule) {
        SystemRuleEntity entity = new SystemRuleEntity();
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        entity.setHighestSystemLoad(rule.getHighestSystemLoad());
        entity.setHighestCpuUsage(rule.getHighestCpuUsage());
        entity.setAvgRt(rule.getAvgRt());
        entity.setMaxThread(rule.getMaxThread());
        entity.setQps(rule.getQps());
        return entity;
    }

    /** Get the IP address of the machine. */
    @Override
    public String getIp() {
        return ip;
    }

    /** Set the IP address of the machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Get the port of the machine. */
    @Override
    public Integer getPort() {
        return port;
    }

    /** Set the port of the machine. */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** Get the entity id. */
    @Override
    public Long getId() {
        return id;
    }

    /** Set the entity id. */
    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /** Get the application name. */
    @Override
    public String getApp() {
        return app;
    }

    /** Set the application name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the highest system load threshold. */
    public Double getHighestSystemLoad() {
        return highestSystemLoad;
    }

    /** Set the highest system load threshold. */
    public void setHighestSystemLoad(Double highestSystemLoad) {
        this.highestSystemLoad = highestSystemLoad;
    }

    /** Get the average RT (ms) threshold for all inbound requests. */
    public Long getAvgRt() {
        return avgRt;
    }

    /** Set the average RT (ms) threshold for all inbound requests. */
    public void setAvgRt(Long avgRt) {
        this.avgRt = avgRt;
    }

    /** Get the max concurrent thread count threshold. */
    public Long getMaxThread() {
        return maxThread;
    }

    /** Set the max concurrent thread count threshold. */
    public void setMaxThread(Long maxThread) {
        this.maxThread = maxThread;
    }

    /** Get the max total QPS of all inbound requests. */
    public Double getQps() {
        return qps;
    }

    /** Set the max total QPS of all inbound requests. */
    public void setQps(Double qps) {
        this.qps = qps;
    }

    /** Get the highest CPU usage threshold. */
    public Double getHighestCpuUsage() {
        return highestCpuUsage;
    }

    /** Set the highest CPU usage threshold. */
    public void setHighestCpuUsage(Double highestCpuUsage) {
        this.highestCpuUsage = highestCpuUsage;
    }

    /** Get the create time. */
    @Override
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

    /** Convert to a {@link SystemRule} with all threshold fields copied. */
    @Override
    public SystemRule toRule() {
        SystemRule rule = new SystemRule();
        rule.setHighestSystemLoad(highestSystemLoad);
        rule.setAvgRt(avgRt);
        rule.setMaxThread(maxThread);
        rule.setQps(qps);
        rule.setHighestCpuUsage(highestCpuUsage);
        return rule;
    }
}
