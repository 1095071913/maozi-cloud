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

import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowClusterConfig;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Entity for a hot parameter flow control rule, wrapping a {@link ParamFlowRule}.
 *
 * @author Eric Zhao
 * @since 0.2.1
 */
public class ParamFlowRuleEntity extends AbstractRuleEntity<ParamFlowRule> {

    /** Create an entity with no wrapped rule. */
    public ParamFlowRuleEntity() {
    }

    /** Create an entity wrapping the given rule, which must not be null. */
    public ParamFlowRuleEntity(ParamFlowRule rule) {
        AssertUtil.notNull(rule, "Authority rule should not be null");
        this.rule = rule;
    }

    /**
     * Build an entity wrapping the given {@link ParamFlowRule}, carrying the given app, ip and port.
     */
    public static ParamFlowRuleEntity fromParamFlowRule(String app, String ip, Integer port, ParamFlowRule rule) {
        ParamFlowRuleEntity entity = new ParamFlowRuleEntity(rule);
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        return entity;
    }

    /** Get the limitApp of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public String getLimitApp() {
        return rule.getLimitApp();
    }

    /** Get the resource name of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public String getResource() {
        return rule.getResource();
    }

    /** Get the threshold type of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getGrade() {
        return rule.getGrade();
    }

    /** Get the index of the parameter the rule applies to (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public Integer getParamIdx() {
        return rule.getParamIdx();
    }

    /** Get the threshold count of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public double getCount() {
        return rule.getCount();
    }

    /** Get the parameter flow item list of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public List<ParamFlowItem> getParamFlowItemList() {
        return rule.getParamFlowItemList();
    }

    /** Get the control behavior of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getControlBehavior() {
        return rule.getControlBehavior();
    }

    /** Get the max queueing timeout of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getMaxQueueingTimeMs() {
        return rule.getMaxQueueingTimeMs();
    }

    /** Get the burst count of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getBurstCount() {
        return rule.getBurstCount();
    }

    /** Get the statistic duration in seconds of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public long getDurationInSec() {
        return rule.getDurationInSec();
    }

    /** Whether the wrapped rule works in cluster mode (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public boolean isClusterMode() {
        return rule.isClusterMode();
    }

    /** Get the cluster mode config of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public ParamFlowClusterConfig getClusterConfig() {
        return rule.getClusterConfig();
    }
}
