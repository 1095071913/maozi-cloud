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

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Authority rules publisher backed by Nacos.
 *
 * @author maozi
 */
@Component("authorityRuleNacosPublisher")
public class AuthorityRuleNacosPublisher implements DynamicRulePublisher<List<AuthorityRuleEntity>> {

    /** Nacos config service used to write rule configs. */
    @Autowired
    private ConfigService configService;
    /** Nacos connection properties, supplies the rule config group. */
    @Autowired
    private NacosProperties nacosProperties;

    /**
     * Push the authority rules of an app to the Nacos config
     * {@code appName + AUTHORITY_DATA_ID_POSTFIX} in the configured group, after
     * converting the entities to {@link AuthorityRule} objects. Does nothing when rules
     * is {@code null}.
     */
    @Override
    public void publish(String app, List<AuthorityRuleEntity> rules) throws Exception {
        if (rules == null) {
            return;
        }
        List<AuthorityRule> authorityRules = rules.stream()
            .map(AuthorityRuleEntity::getRule)
            .collect(Collectors.toList());
        NacosConfigUtil.publishJsonConfig(configService, app,
            NacosConfigUtil.AUTHORITY_DATA_ID_POSTFIX, nacosProperties.getGroup(), authorityRules);
    }
}
