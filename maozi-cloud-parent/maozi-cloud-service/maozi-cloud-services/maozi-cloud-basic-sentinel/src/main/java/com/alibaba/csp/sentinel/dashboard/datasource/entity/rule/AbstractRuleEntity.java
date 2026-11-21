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

import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base entity holding a wrapped {@link AbstractRule} plus common fields shared by rule entities.
 *
 * @author Eric Zhao
 * @since 0.2.1
 */
public abstract class AbstractRuleEntity<T extends AbstractRule> extends BaseRuleEntity {

    /** The wrapped rule object. */
    protected T rule;

    /** Creates an entity with no wrapped rule. */
    protected AbstractRuleEntity() {
    }

    /** Creates an entity wrapping the given rule, which must not be null. */
    protected AbstractRuleEntity(T rule) {
        AssertUtil.notNull(rule, "rule should not be null");
        this.rule = rule;
    }

    /** Creates an entity wrapping the given rule and carrying the given app, ip and port. */
    protected AbstractRuleEntity(String app, String ip, Integer port, T rule) {
        this(rule);
        setApp(app);
        setIp(ip);
        setPort(port);
    }

    /** Sets the application name and returns this. */
    @Override
    public AbstractRuleEntity<T> setApp(String app) {
        this.app = app;
        return this;
    }

    /** Sets the IP address of the machine and returns this. */
    @Override
    public AbstractRuleEntity<T> setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Sets the port of the machine and returns this. */
    @Override
    public AbstractRuleEntity<T> setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** Gets the wrapped rule object. */
    public T getRule() {
        return rule;
    }

    /** Sets the wrapped rule object and returns this. */
    public AbstractRuleEntity<T> setRule(T rule) {
        this.rule = rule;
        return this;
    }

    /** Gets the resource name of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public String getResource() {
        return rule.getResource();
    }

    /** Gets the limitApp of the wrapped rule (excluded from JSON). */
    @JsonIgnore
    @JSONField(serialize = false)
    public String getLimitApp() {
        return rule.getLimitApp();
    }

    /** Returns the wrapped rule object directly. */
    @Override
    public T toRule() {
        return rule;
    }

    @Override
    public String toString() {
        return "AbstractRuleEntity{"
                + "id=" + id
                + ", app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", rule=" + rule
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + '}';
    }
}
