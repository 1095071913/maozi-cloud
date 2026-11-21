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
 * 新增/修改网关流控规则请求的公共基类 VO，持有两种请求共有的字段
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public abstract class BaseFlowRuleReqVO {

    /** 目标应用名称 */
    protected String app;

    /** 规则的阈值类型，0 表示 QPS（见 RuleConstant） */
    protected Integer grade;

    /** 流控规则阈值 */
    protected Double count;

    /** 统计间隔，单位由 intervalUnit 决定 */
    protected Long interval;

    /** 统计间隔的时间单位（秒、分、时、天） */
    protected Integer intervalUnit;

    /** 阈值超出后的流控效果，如直接拒绝或匀速排队 */
    protected Integer controlBehavior;

    /** 阈值超出时允许额外放行的突发请求数 */
    protected Integer burst;

    /** 流控效果为匀速排队时的最大排队超时时间（毫秒） */
    protected Integer maxQueueingTimeoutMs;

    /** 热点参数规则项，普通网关流控规则时为 null */
    protected GatewayParamFlowItemVO paramItem;

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public Double getCount() {
        return count;
    }

    public void setCount(Double count) {
        this.count = count;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Integer getIntervalUnit() {
        return intervalUnit;
    }

    public void setIntervalUnit(Integer intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    public Integer getControlBehavior() {
        return controlBehavior;
    }

    public void setControlBehavior(Integer controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    public Integer getBurst() {
        return burst;
    }

    public void setBurst(Integer burst) {
        this.burst = burst;
    }

    public Integer getMaxQueueingTimeoutMs() {
        return maxQueueingTimeoutMs;
    }

    public void setMaxQueueingTimeoutMs(Integer maxQueueingTimeoutMs) {
        this.maxQueueingTimeoutMs = maxQueueingTimeoutMs;
    }

    public GatewayParamFlowItemVO getParamItem() {
        return paramItem;
    }

    public void setParamItem(GatewayParamFlowItemVO paramItem) {
        this.paramItem = paramItem;
    }
}
