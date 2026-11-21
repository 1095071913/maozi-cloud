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
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 热点参数限流规则实体，内部包装 {@link ParamFlowRule}。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
public class ParamFlowRuleEntity extends AbstractRuleEntity<ParamFlowRule> {

    /** 创建一个不携带规则的实体。 */
    public ParamFlowRuleEntity() {
        super();
    }

    /** 创建包装指定规则的实体，规则不能为空。 */
    public ParamFlowRuleEntity(ParamFlowRule rule) {
        super(rule);
    }

    /** 创建包装指定规则并携带应用名、IP、端口的实体。 */
    public ParamFlowRuleEntity(String app, String ip, Integer port, ParamFlowRule rule) {
        super(app, ip, port, rule);
    }

    /**
     * 根据热点参数限流规则构建实体。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器端口
     * @param rule 热点参数限流规则
     * @return 携带应用名、IP、端口并包装该规则的实体
     */
    public static ParamFlowRuleEntity fromParamFlowRule(String app, String ip, Integer port, ParamFlowRule rule) {
        return new ParamFlowRuleEntity(app, ip, port, rule);
    }

    /** 获取规则的阈值类型（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getGrade() {
        return rule.getGrade();
    }

    /** 获取规则作用的参数下标（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public Integer getParamIdx() {
        return rule.getParamIdx();
    }

    /** 获取规则的限流阈值（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public double getCount() {
        return rule.getCount();
    }

    /** 获取规则的热点参数例外项列表（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public List<ParamFlowItem> getParamFlowItemList() {
        return rule.getParamFlowItemList();
    }

    /** 获取规则的流控效果（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getControlBehavior() {
        return rule.getControlBehavior();
    }

    /** 获取规则的最大排队超时时间（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getMaxQueueingTimeMs() {
        return rule.getMaxQueueingTimeMs();
    }

    /** 获取规则的突发增量数（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getBurstCount() {
        return rule.getBurstCount();
    }

    /** 获取规则的统计时长，单位秒（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public long getDurationInSec() {
        return rule.getDurationInSec();
    }

    /** 规则是否集群流控模式（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public boolean isClusterMode() {
        return rule.isClusterMode();
    }

    /** 获取规则的集群流控配置（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public ParamFlowClusterConfig getClusterConfig() {
        return rule.getClusterConfig();
    }
}
