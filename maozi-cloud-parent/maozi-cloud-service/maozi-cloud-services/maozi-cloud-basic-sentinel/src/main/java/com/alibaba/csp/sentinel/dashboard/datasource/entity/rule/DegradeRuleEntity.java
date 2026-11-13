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

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;

import java.util.Date;

/**
 * Entity for a circuit breaking (degrade) rule.
 *
 * @author leyou
 */
public class DegradeRuleEntity implements RuleEntity {

    /** The entity id. */
    private Long id;
    /** The application name. */
    private String app;

    /** The IP address of the machine. */
    private String ip;
    /** The port of the machine. */
    private Integer port;

    /** The resource name. */
    private String resource;
    /** The caller that the rule applies to. */
    private String limitApp;
    /** The threshold, whose meaning depends on grade (RT in ms, exception ratio or exception count). */
    private Double count;
    /** The degrade time window in seconds. */
    private Integer timeWindow;
    /** The circuit breaking strategy: 0 RT, 1 exception ratio and 2 exception count. */
    private Integer grade;
    /** The min request amount that triggers degrading. */
    private Integer minRequestAmount;
    /** The slow call ratio threshold of the rule. */
    private Double slowRatioThreshold;
    /** The statistic interval in milliseconds. */
    private Integer statIntervalMs;

    /** The create time. */
    private Date gmtCreate;
    /** The last modify time. */
    private Date gmtModified;

    /**
     * Build an entity from the given {@link DegradeRule}, carrying the given app, ip and port.
     */
    public static DegradeRuleEntity fromDegradeRule(String app, String ip, Integer port, DegradeRule rule) {
        DegradeRuleEntity entity = new DegradeRuleEntity();
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        entity.setResource(rule.getResource());
        entity.setLimitApp(rule.getLimitApp());
        entity.setCount(rule.getCount());
        entity.setTimeWindow(rule.getTimeWindow());
        entity.setGrade(rule.getGrade());
        entity.setMinRequestAmount(rule.getMinRequestAmount());
        entity.setSlowRatioThreshold(rule.getSlowRatioThreshold());
        entity.setStatIntervalMs(rule.getStatIntervalMs());
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

    /** Get the resource name. */
    public String getResource() {
        return resource;
    }

    /** Set the resource name. */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Get the caller that the rule applies to. */
    public String getLimitApp() {
        return limitApp;
    }

    /** Set the caller that the rule applies to. */
    public void setLimitApp(String limitApp) {
        this.limitApp = limitApp;
    }

    /** Get the threshold count. */
    public Double getCount() {
        return count;
    }

    /** Set the threshold count. */
    public void setCount(Double count) {
        this.count = count;
    }

    /** Get the degrade time window in seconds. */
    public Integer getTimeWindow() {
        return timeWindow;
    }

    /** Set the degrade time window in seconds. */
    public void setTimeWindow(Integer timeWindow) {
        this.timeWindow = timeWindow;
    }

    /** Get the circuit breaking strategy. */
    public Integer getGrade() {
        return grade;
    }

    /** Set the circuit breaking strategy. */
    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    /** Get the min request amount that triggers degrading. */
    public Integer getMinRequestAmount() {
        return minRequestAmount;
    }

    /** Set the min request amount that triggers degrading and return this. */
    public DegradeRuleEntity setMinRequestAmount(Integer minRequestAmount) {
        this.minRequestAmount = minRequestAmount;
        return this;
    }

    /** Get the slow call ratio threshold of the rule. */
    public Double getSlowRatioThreshold() {
        return slowRatioThreshold;
    }

    /** Set the slow call ratio threshold of the rule and return this. */
    public DegradeRuleEntity setSlowRatioThreshold(Double slowRatioThreshold) {
        this.slowRatioThreshold = slowRatioThreshold;
        return this;
    }

    /** Get the statistic interval in milliseconds. */
    public Integer getStatIntervalMs() {
        return statIntervalMs;
    }

    /** Set the statistic interval in milliseconds and return this. */
    public DegradeRuleEntity setStatIntervalMs(Integer statIntervalMs) {
        this.statIntervalMs = statIntervalMs;
        return this;
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

    /**
     * Convert to a {@link DegradeRule}, only setting minRequestAmount, slowRatioThreshold
     * and statIntervalMs when they are not null.
     */
    @Override
    public DegradeRule toRule() {
        DegradeRule rule = new DegradeRule();
        rule.setResource(resource);
        rule.setLimitApp(limitApp);
        rule.setCount(count);
        rule.setTimeWindow(timeWindow);
        rule.setGrade(grade);
        if (minRequestAmount != null) {
            rule.setMinRequestAmount(minRequestAmount);
        }
        if (slowRatioThreshold != null) {
            rule.setSlowRatioThreshold(slowRatioThreshold);
        }
        if (statIntervalMs != null) {
            rule.setStatIntervalMs(statIntervalMs);
        }

        return rule;
    }
}
