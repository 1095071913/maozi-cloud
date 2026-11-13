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

import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;

import java.util.Date;

/**
 * Entity for a flow (QPS or thread count) rule.
 *
 * @author leyou
 */
public class FlowRuleEntity implements RuleEntity {

    /** The entity id. */
    private Long id;
    /** The application name. */
    private String app;
    /** The IP address of the machine. */
    private String ip;
    /** The port of the machine. */
    private Integer port;
    /** The caller that the rule applies to. */
    private String limitApp;
    /** The resource name. */
    private String resource;
    /** The threshold type: 0 for thread count and 1 for QPS. */
    private Integer grade;
    /** The threshold count of the rule. */
    private Double count;
    /** The flow control strategy: 0 direct, 1 related and 2 chain. */
    private Integer strategy;
    /** The related resource for association and chain strategy. */
    private String refResource;
    /** The control behavior: 0 default, 1 warm up and 2 rate limiter. */
    private Integer controlBehavior;
    /** The warm up period in seconds for warm up behavior. */
    private Integer warmUpPeriodSec;
    /** The max queueing time in milliseconds for rate limiter behavior. */
    private Integer maxQueueingTimeMs;

    /** Whether the rule works in cluster mode. */
    private boolean clusterMode;
    /** The flow rule config for cluster mode. */
    private ClusterFlowConfig clusterConfig;

    /** The create time. */
    private Date gmtCreate;
    /** The last modify time. */
    private Date gmtModified;

    /**
     * Build an entity from the given {@link FlowRule}, carrying the given app, ip and port.
     */
    public static FlowRuleEntity fromFlowRule(String app, String ip, Integer port, FlowRule rule) {
        FlowRuleEntity entity = new FlowRuleEntity();
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        entity.setLimitApp(rule.getLimitApp());
        entity.setResource(rule.getResource());
        entity.setGrade(rule.getGrade());
        entity.setCount(rule.getCount());
        entity.setStrategy(rule.getStrategy());
        entity.setRefResource(rule.getRefResource());
        entity.setControlBehavior(rule.getControlBehavior());
        entity.setWarmUpPeriodSec(rule.getWarmUpPeriodSec());
        entity.setMaxQueueingTimeMs(rule.getMaxQueueingTimeMs());
        entity.setClusterMode(rule.isClusterMode());
        entity.setClusterConfig(rule.getClusterConfig());
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

    /** Get the application name. */
    @Override
    public String getApp() {
        return app;
    }

    /** Set the application name. */
    public void setApp(String app) {
        this.app = app;
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

    /** Get the caller that the rule applies to. */
    public String getLimitApp() {
        return limitApp;
    }

    /** Set the caller that the rule applies to. */
    public void setLimitApp(String limitApp) {
        this.limitApp = limitApp;
    }

    /** Get the resource name. */
    public String getResource() {
        return resource;
    }

    /** Set the resource name. */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Get the threshold type: 0 for thread count and 1 for QPS. */
    public Integer getGrade() {
        return grade;
    }

    /** Set the threshold type: 0 for thread count and 1 for QPS. */
    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    /** Get the threshold count of the rule. */
    public Double getCount() {
        return count;
    }

    /** Set the threshold count of the rule. */
    public void setCount(Double count) {
        this.count = count;
    }

    /** Get the flow control strategy. */
    public Integer getStrategy() {
        return strategy;
    }

    /** Set the flow control strategy. */
    public void setStrategy(Integer strategy) {
        this.strategy = strategy;
    }

    /** Get the related resource for association and chain strategy. */
    public String getRefResource() {
        return refResource;
    }

    /** Set the related resource for association and chain strategy. */
    public void setRefResource(String refResource) {
        this.refResource = refResource;
    }

    /** Get the control behavior. */
    public Integer getControlBehavior() {
        return controlBehavior;
    }

    /** Set the control behavior. */
    public void setControlBehavior(Integer controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    /** Get the warm up period in seconds. */
    public Integer getWarmUpPeriodSec() {
        return warmUpPeriodSec;
    }

    /** Set the warm up period in seconds. */
    public void setWarmUpPeriodSec(Integer warmUpPeriodSec) {
        this.warmUpPeriodSec = warmUpPeriodSec;
    }

    /** Get the max queueing time in milliseconds. */
    public Integer getMaxQueueingTimeMs() {
        return maxQueueingTimeMs;
    }

    /** Set the max queueing time in milliseconds. */
    public void setMaxQueueingTimeMs(Integer maxQueueingTimeMs) {
        this.maxQueueingTimeMs = maxQueueingTimeMs;
    }

    /** Whether the rule works in cluster mode. */
    public boolean isClusterMode() {
        return clusterMode;
    }

    /** Set whether the rule works in cluster mode and return this. */
    public FlowRuleEntity setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
        return this;
    }

    /** Get the flow rule config for cluster mode. */
    public ClusterFlowConfig getClusterConfig() {
        return clusterConfig;
    }

    /** Set the flow rule config for cluster mode and return this. */
    public FlowRuleEntity setClusterConfig(ClusterFlowConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
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
     * Convert to a {@link FlowRule}, only setting controlBehavior, warmUpPeriodSec and
     * maxQueueingTimeMs when they are not null.
     */
    @Override
    public FlowRule toRule() {
        FlowRule flowRule = new FlowRule();
        flowRule.setCount(this.count);
        flowRule.setGrade(this.grade);
        flowRule.setResource(this.resource);
        flowRule.setLimitApp(this.limitApp);
        flowRule.setRefResource(this.refResource);
        flowRule.setStrategy(this.strategy);
        if (this.controlBehavior != null) {
            flowRule.setControlBehavior(controlBehavior);
        }
        if (this.warmUpPeriodSec != null) {
            flowRule.setWarmUpPeriodSec(warmUpPeriodSec);
        }
        if (this.maxQueueingTimeMs != null) {
            flowRule.setMaxQueueingTimeMs(maxQueueingTimeMs);
        }
        flowRule.setClusterMode(clusterMode);
        flowRule.setClusterConfig(clusterConfig);
        return flowRule;
    }

}
