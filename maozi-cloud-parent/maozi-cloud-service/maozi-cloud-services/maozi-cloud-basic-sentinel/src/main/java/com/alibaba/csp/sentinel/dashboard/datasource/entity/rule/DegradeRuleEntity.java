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

/**
 * Entity for a circuit breaking (degrade) rule.
 *
 * @author leyou
 * @date 2026/11/22
 */
public class DegradeRuleEntity extends BaseRuleEntity {

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

    /**
     * Builds an entity from the given {@link DegradeRule}, carrying the given app, ip and port.
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

    /** Gets the resource name. */
    public String getResource() {
        return resource;
    }

    /** Sets the resource name. */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Gets the caller that the rule applies to. */
    public String getLimitApp() {
        return limitApp;
    }

    /** Sets the caller that the rule applies to. */
    public void setLimitApp(String limitApp) {
        this.limitApp = limitApp;
    }

    /** Gets the threshold count. */
    public Double getCount() {
        return count;
    }

    /** Sets the threshold count. */
    public void setCount(Double count) {
        this.count = count;
    }

    /** Gets the degrade time window in seconds. */
    public Integer getTimeWindow() {
        return timeWindow;
    }

    /** Sets the degrade time window in seconds. */
    public void setTimeWindow(Integer timeWindow) {
        this.timeWindow = timeWindow;
    }

    /** Gets the circuit breaking strategy. */
    public Integer getGrade() {
        return grade;
    }

    /** Sets the circuit breaking strategy. */
    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    /** Gets the min request amount that triggers degrading. */
    public Integer getMinRequestAmount() {
        return minRequestAmount;
    }

    /** Sets the min request amount that triggers degrading and returns this. */
    public DegradeRuleEntity setMinRequestAmount(Integer minRequestAmount) {
        this.minRequestAmount = minRequestAmount;
        return this;
    }

    /** Gets the slow call ratio threshold of the rule. */
    public Double getSlowRatioThreshold() {
        return slowRatioThreshold;
    }

    /** Sets the slow call ratio threshold of the rule and returns this. */
    public DegradeRuleEntity setSlowRatioThreshold(Double slowRatioThreshold) {
        this.slowRatioThreshold = slowRatioThreshold;
        return this;
    }

    /** Gets the statistic interval in milliseconds. */
    public Integer getStatIntervalMs() {
        return statIntervalMs;
    }

    /** Sets the statistic interval in milliseconds and returns this. */
    public DegradeRuleEntity setStatIntervalMs(Integer statIntervalMs) {
        this.statIntervalMs = statIntervalMs;
        return this;
    }

    /**
     * Converts to a {@link DegradeRule}, only setting minRequestAmount, slowRatioThreshold
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

    /** Return a string representation of this entity. */
    @Override
    public String toString() {
        return "DegradeRuleEntity{"
                + "id=" + id
                + ", app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", resource='" + resource + '\''
                + ", limitApp='" + limitApp + '\''
                + ", count=" + count
                + ", timeWindow=" + timeWindow
                + ", grade=" + grade
                + ", minRequestAmount=" + minRequestAmount
                + ", slowRatioThreshold=" + slowRatioThreshold
                + ", statIntervalMs=" + statIntervalMs
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + '}';
    }
}
