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
package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 Nacos 的热点参数流控规则提供者
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component("paramFlowRuleNacosProvider")
public class ParamFlowRuleNacosProvider implements DynamicRuleProvider<List<ParamFlowRuleEntity>> {

    /** 用于读取规则配置的 Nacos 配置服务 */
    private final ConfigService configService;
    /** Nacos 连接配置，提供规则配置所在分组 */
    private final NacosProperties nacosProperties;

    /** 使用给定的配置服务与 Nacos 配置创建提供者 */
    public ParamFlowRuleNacosProvider(ConfigService configService, NacosProperties nacosProperties) {
        this.configService = configService;
        this.nacosProperties = nacosProperties;
    }

    /**
     * 从配置分组中的 Nacos 配置 {@code appName + PARAM_FLOW_DATA_ID_POSTFIX}
     * 加载应用的热点参数流控规则，解析 JSON 内容并转换为实体（ip 与端口留空）。
     * 配置为空或不存在时返回空列表
     */
    @Override
    public List<ParamFlowRuleEntity> getRules(String appName) throws Exception {
        String rules = NacosConfigUtil.readFreshConfig(configService, appName,
            NacosConfigUtil.PARAM_FLOW_DATA_ID_POSTFIX, nacosProperties.getGroup(), NacosConfigUtil.READ_TIMEOUT_MS);
        if (StringUtil.isEmpty(rules)) {
            return new ArrayList<>();
        }
        List<ParamFlowRule> list = JSON.parseArray(rules, ParamFlowRule.class);
        return list.stream()
            .map(rule -> ParamFlowRuleEntity.fromParamFlowRule(appName, null, null, rule))
            .collect(Collectors.toList());
    }
}
