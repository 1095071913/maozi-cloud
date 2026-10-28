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
package com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule;

/**
 * Value Object for add gateway flow rule.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class AddFlowRuleReqVo {

    /** Name of the target application. */
    private String app;

    /** IP of the Sentinel client machine that the rule belongs to. */
    private String ip;

    /** Port of the Sentinel client machine that the rule belongs to. */
    private Integer port;

    /** Resource of the rule: route ID or API name, depending on resourceMode. */
    private String resource;

    /** Resource mode: 0 for route ID, 1 for custom API name (see SentinelGatewayConstants). */
    private Integer resourceMode;

    /** Metric grade of the rule, 0 means QPS (see RuleConstant). */
    private Integer grade;

    /** Threshold of the flow rule. */
    private Double count;

    /** Stat interval of the flow rule, in the unit given by intervalUnit. */
    private Long interval;

    /** Time unit of the stat interval (second, minute, hour or day). */
    private Integer intervalUnit;

    /** Control behavior when the threshold is exceeded, e.g. reject or uniform queueing. */
    private Integer controlBehavior;

    /** Number of extra requests allowed to pass in a burst when the threshold is exceeded. */
    private Integer burst;

    /** Max queueing timeout in milliseconds when control behavior is uniform queueing. */
    private Integer maxQueueingTimeoutMs;

    /** Hotspot parameter item of the rule, null for a plain gateway flow rule. */
    private GatewayParamFlowItemVo paramItem;

    /** Returns the name of the target application. */
    public String getApp() {
        return app;
    }

    /** Sets the name of the target application. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Returns the IP of the Sentinel client machine. */
    public String getIp() {
        return ip;
    }

    /** Sets the IP of the Sentinel client machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Returns the port of the Sentinel client machine. */
    public Integer getPort() {
        return port;
    }

    /** Sets the port of the Sentinel client machine. */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** Returns the resource (route ID or API name). */
    public String getResource() {
        return resource;
    }

    /** Sets the resource (route ID or API name). */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Returns the resource mode (0 route ID, 1 custom API name). */
    public Integer getResourceMode() {
        return resourceMode;
    }

    /** Sets the resource mode (0 route ID, 1 custom API name). */
    public void setResourceMode(Integer resourceMode) {
        this.resourceMode = resourceMode;
    }

    /** Returns the metric grade of the rule. */
    public Integer getGrade() {
        return grade;
    }

    /** Sets the metric grade of the rule. */
    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    /** Returns the threshold of the flow rule. */
    public Double getCount() {
        return count;
    }

    /** Sets the threshold of the flow rule. */
    public void setCount(Double count) {
        this.count = count;
    }

    /** Returns the stat interval of the flow rule. */
    public Long getInterval() {
        return interval;
    }

    /** Sets the stat interval of the flow rule. */
    public void setInterval(Long interval) {
        this.interval = interval;
    }

    /** Returns the time unit of the stat interval. */
    public Integer getIntervalUnit() {
        return intervalUnit;
    }

    /** Sets the time unit of the stat interval. */
    public void setIntervalUnit(Integer intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    /** Returns the control behavior of the flow rule. */
    public Integer getControlBehavior() {
        return controlBehavior;
    }

    /** Sets the control behavior of the flow rule. */
    public void setControlBehavior(Integer controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    /** Returns the allowed burst size above the threshold. */
    public Integer getBurst() {
        return burst;
    }

    /** Sets the allowed burst size above the threshold. */
    public void setBurst(Integer burst) {
        this.burst = burst;
    }

    /** Returns the max queueing timeout in milliseconds. */
    public Integer getMaxQueueingTimeoutMs() {
        return maxQueueingTimeoutMs;
    }

    /** Sets the max queueing timeout in milliseconds. */
    public void setMaxQueueingTimeoutMs(Integer maxQueueingTimeoutMs) {
        this.maxQueueingTimeoutMs = maxQueueingTimeoutMs;
    }

    /** Returns the hotspot parameter item of the rule. */
    public GatewayParamFlowItemVo getParamItem() {
        return paramItem;
    }

    /** Sets the hotspot parameter item of the rule. */
    public void setParamItem(GatewayParamFlowItemVo paramItem) {
        this.paramItem = paramItem;
    }
}
