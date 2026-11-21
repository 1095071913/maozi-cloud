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

import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 授权（黑白名单）规则实体，内部包装 {@link AuthorityRule}。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
public class AuthorityRuleEntity extends AbstractRuleEntity<AuthorityRule> {

    /** 创建一个不携带规则的实体。 */
    public AuthorityRuleEntity() {
        super();
    }

    /** 创建包装指定规则的实体，规则不能为空。 */
    public AuthorityRuleEntity(AuthorityRule authorityRule) {
        super(authorityRule);
    }

    /** 创建包装指定规则并携带应用名、IP、端口的实体。 */
    public AuthorityRuleEntity(String app, String ip, Integer port, AuthorityRule rule) {
        super(app, ip, port, rule);
    }

    /**
     * 根据授权规则构建实体。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器端口
     * @param rule 授权规则
     * @return 携带应用名、IP、端口并包装该规则的实体
     */
    public static AuthorityRuleEntity fromAuthorityRule(String app, String ip, Integer port, AuthorityRule rule) {
        return new AuthorityRuleEntity(app, ip, port, rule);
    }

    /** 获取规则的控制策略（白名单 0 / 黑名单 1），该字段不参与 JSON 序列化。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getStrategy() {
        return rule.getStrategy();
    }
}
