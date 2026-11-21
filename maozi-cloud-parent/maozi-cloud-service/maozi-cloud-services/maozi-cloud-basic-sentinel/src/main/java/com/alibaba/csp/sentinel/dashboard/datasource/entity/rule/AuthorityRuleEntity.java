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
 * Entity for an authority (black/white list) rule, wrapping an {@link AuthorityRule}.
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
public class AuthorityRuleEntity extends AbstractRuleEntity<AuthorityRule> {

    /** Creates an entity with no wrapped rule. */
    public AuthorityRuleEntity() {
        super();
    }

    /** Creates an entity wrapping the given rule, which must not be null. */
    public AuthorityRuleEntity(AuthorityRule authorityRule) {
        super(authorityRule);
    }

    /** Creates an entity wrapping the given rule and carrying the given app, ip and port. */
    public AuthorityRuleEntity(String app, String ip, Integer port, AuthorityRule rule) {
        super(app, ip, port, rule);
    }

    /**
     * Builds an entity wrapping the given {@link AuthorityRule}, carrying the given app, ip and port.
     */
    public static AuthorityRuleEntity fromAuthorityRule(String app, String ip, Integer port, AuthorityRule rule) {
        return new AuthorityRuleEntity(app, ip, port, rule);
    }

    /** Gets the strategy (white/black list) of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public int getStrategy() {
        return rule.getStrategy();
    }
}
