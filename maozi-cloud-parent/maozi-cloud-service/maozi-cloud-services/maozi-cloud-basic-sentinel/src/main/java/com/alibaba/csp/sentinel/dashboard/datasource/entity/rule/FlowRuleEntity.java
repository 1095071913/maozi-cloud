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

/**
 * 流控（QPS 或并发线程数）规则实体。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class FlowRuleEntity extends BaseRuleEntity {

    /** 规则针对的调用来源应用。 */
    private String limitApp;
    /** 资源名。 */
    private String resource;
    /** 阈值类型：0 并发线程数，1 QPS。 */
    private Integer grade;
    /** 阈值。 */
    private Double count;
    /** 流控策略：0 直接，1 关联，2 链路。 */
    private Integer strategy;
    /** 关联、链路策略对应的关联资源。 */
    private String refResource;
    /** 流控效果：0 默认（快速失败），1 Warm Up 预热，2 匀速排队。 */
    private Integer controlBehavior;
    /** Warm Up 预热时长，单位秒。 */
    private Integer warmUpPeriodSec;
    /** 匀速排队模式下的最大排队时长，单位毫秒。 */
    private Integer maxQueueingTimeMs;

    /** 是否集群流控模式。 */
    private boolean clusterMode;
    /** 集群流控相关配置。 */
    private ClusterFlowConfig clusterConfig;

    /**
     * 根据 {@link FlowRule} 构建携带指定应用名、IP、端口的实体。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器端口
     * @param rule 流控规则
     * @return 由规则字段填充而成的实体
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

    /** 获取规则针对的调用来源应用。 */
    public String getLimitApp() {
        return limitApp;
    }

    /** 设置规则针对的调用来源应用。 */
    public void setLimitApp(String limitApp) {
        this.limitApp = limitApp;
    }

    /** 获取资源名。 */
    public String getResource() {
        return resource;
    }

    /** 设置资源名。 */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** 获取阈值类型：0 并发线程数，1 QPS。 */
    public Integer getGrade() {
        return grade;
    }

    /** 设置阈值类型：0 并发线程数，1 QPS。 */
    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    /** 获取阈值。 */
    public Double getCount() {
        return count;
    }

    /** 设置阈值。 */
    public void setCount(Double count) {
        this.count = count;
    }

    /** 获取流控策略。 */
    public Integer getStrategy() {
        return strategy;
    }

    /** 设置流控策略。 */
    public void setStrategy(Integer strategy) {
        this.strategy = strategy;
    }

    /** 获取关联、链路策略对应的关联资源。 */
    public String getRefResource() {
        return refResource;
    }

    /** 设置关联、链路策略对应的关联资源。 */
    public void setRefResource(String refResource) {
        this.refResource = refResource;
    }

    /** 获取流控效果。 */
    public Integer getControlBehavior() {
        return controlBehavior;
    }

    /** 设置流控效果。 */
    public void setControlBehavior(Integer controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    /** 获取 Warm Up 预热时长，单位秒。 */
    public Integer getWarmUpPeriodSec() {
        return warmUpPeriodSec;
    }

    /** 设置 Warm Up 预热时长，单位秒。 */
    public void setWarmUpPeriodSec(Integer warmUpPeriodSec) {
        this.warmUpPeriodSec = warmUpPeriodSec;
    }

    /** 获取最大排队时长，单位毫秒。 */
    public Integer getMaxQueueingTimeMs() {
        return maxQueueingTimeMs;
    }

    /** 设置最大排队时长，单位毫秒。 */
    public void setMaxQueueingTimeMs(Integer maxQueueingTimeMs) {
        this.maxQueueingTimeMs = maxQueueingTimeMs;
    }

    /** 是否集群流控模式。 */
    public boolean isClusterMode() {
        return clusterMode;
    }

    /** 设置是否集群流控模式并返回当前对象。 */
    public FlowRuleEntity setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
        return this;
    }

    /** 获取集群流控相关配置。 */
    public ClusterFlowConfig getClusterConfig() {
        return clusterConfig;
    }

    /** 设置集群流控相关配置并返回当前对象。 */
    public FlowRuleEntity setClusterConfig(ClusterFlowConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
        return this;
    }

    /**
     * 转换为 {@link FlowRule}，controlBehavior、warmUpPeriodSec、maxQueueingTimeMs
     * 仅在非空时设置。
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

    /** 返回实体的字符串表示。 */
    @Override
    public String toString() {
        return "FlowRuleEntity{"
                + "id=" + id
                + ", app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", limitApp='" + limitApp + '\''
                + ", resource='" + resource + '\''
                + ", grade=" + grade
                + ", count=" + count
                + ", strategy=" + strategy
                + ", refResource='" + refResource + '\''
                + ", controlBehavior=" + controlBehavior
                + ", warmUpPeriodSec=" + warmUpPeriodSec
                + ", maxQueueingTimeMs=" + maxQueueingTimeMs
                + ", clusterMode=" + clusterMode
                + ", clusterConfig=" + clusterConfig
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + '}';
    }
}
