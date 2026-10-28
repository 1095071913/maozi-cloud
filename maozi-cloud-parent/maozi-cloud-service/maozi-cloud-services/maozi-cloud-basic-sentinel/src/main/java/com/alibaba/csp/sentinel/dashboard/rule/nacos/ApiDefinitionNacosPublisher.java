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

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gateway api definitions publisher backed by Nacos.
 *
 * @author maozi
 */
@Component("apiDefinitionNacosPublisher")
public class ApiDefinitionNacosPublisher implements DynamicRulePublisher<List<ApiDefinitionEntity>> {

    /** Nacos config service used to write rule configs. */
    @Autowired
    private ConfigService configService;
    /** Nacos connection properties, supplies the rule config group. */
    @Autowired
    private NacosProperties nacosProperties;

    /**
     * Push the gateway API definitions of an app to the Nacos config
     * {@code appName + GATEWAY_API_DATA_ID_POSTFIX} in the configured group, after
     * converting the entities to {@link ApiDefinition} objects. Does nothing when rules
     * is {@code null}.
     */
    @Override
    public void publish(String app, List<ApiDefinitionEntity> rules) throws Exception {
        if (rules == null) {
            return;
        }
        List<ApiDefinition> apiDefinitions = rules.stream()
            .map(ApiDefinitionEntity::toApiDefinition)
            .collect(Collectors.toList());
        NacosConfigUtil.publishJsonConfig(configService, app,
            NacosConfigUtil.GATEWAY_API_DATA_ID_POSTFIX, nacosProperties.getGroup(), apiDefinitions);
    }
}
