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
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gateway api definitions provider backed by Nacos.
 *
 * @author maozi
 */
@Component("apiDefinitionNacosProvider")
public class ApiDefinitionNacosProvider implements DynamicRuleProvider<List<ApiDefinitionEntity>> {

    /** Nacos config service used to read rule configs. */
    @Autowired
    private ConfigService configService;
    /** Nacos connection properties, supplies the rule config group. */
    @Autowired
    private NacosProperties nacosProperties;

    /**
     * Load the gateway API definitions of an app from the Nacos config
     * {@code appName + GATEWAY_API_DATA_ID_POSTFIX} in the configured group, parse the
     * JSON content and convert it to entities (ip and port left {@code null}). Returns
     * an empty list when the config is empty or absent.
     */
    @Override
    public List<ApiDefinitionEntity> getRules(String appName) throws Exception {
        String rules = NacosConfigUtil.readFreshConfig(configService, appName,
            NacosConfigUtil.GATEWAY_API_DATA_ID_POSTFIX, nacosProperties.getGroup(), 3000);
        if (StringUtil.isEmpty(rules)) {
            return new ArrayList<>();
        }
        List<ApiDefinition> list = JSON.parseArray(rules, ApiDefinition.class);
        return list.stream()
            .map(api -> ApiDefinitionEntity.fromApiDefinition(appName, null, null, api))
            .collect(Collectors.toList());
    }
}
