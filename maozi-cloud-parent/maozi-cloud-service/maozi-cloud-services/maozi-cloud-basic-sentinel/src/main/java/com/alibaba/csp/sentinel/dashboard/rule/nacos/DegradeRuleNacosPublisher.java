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

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 Nacos 的熔断规则发布者
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component("degradeRuleNacosPublisher")
public class DegradeRuleNacosPublisher implements DynamicRulePublisher<List<DegradeRuleEntity>> {

    /** 用于写入规则配置的 Nacos 配置服务 */
    private final ConfigService configService;
    /** Nacos 连接配置，提供规则配置所在分组 */
    private final NacosProperties nacosProperties;

    /** 使用给定的配置服务与 Nacos 配置创建发布者 */
    public DegradeRuleNacosPublisher(ConfigService configService, NacosProperties nacosProperties) {
        this.configService = configService;
        this.nacosProperties = nacosProperties;
    }

    /**
     * 将实体转换为 {@link DegradeRule} 后，把应用的熔断规则推送到
     * 配置分组中的 Nacos 配置 {@code appName + DEGRADE_DATA_ID_POSTFIX}。
     * rules 为 {@code null} 时不做任何操作
     */
    @Override
    public void publish(String app, List<DegradeRuleEntity> rules) throws Exception {
        if (rules == null) {
            return;
        }
        List<DegradeRule> degradeRules = rules.stream()
                .map(DegradeRuleEntity::toRule)
                .collect(Collectors.toList());
        NacosConfigUtil.publishJsonConfig(configService, app,
                NacosConfigUtil.DEGRADE_DATA_ID_POSTFIX, nacosProperties.getGroup(), degradeRules);
    }
}
