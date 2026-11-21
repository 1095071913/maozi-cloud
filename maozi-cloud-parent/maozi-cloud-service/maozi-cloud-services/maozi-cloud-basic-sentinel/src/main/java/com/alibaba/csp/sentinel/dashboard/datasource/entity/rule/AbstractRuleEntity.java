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
 * 持有被包装 {@link AbstractRule} 及规则实体公共字段的基类。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
public abstract class AbstractRuleEntity<T extends AbstractRule> extends BaseRuleEntity {

    /** 被包装的规则对象。 */
    protected T rule;

    /** 构造不包装规则的实体。 */
    protected AbstractRuleEntity() {
    }

    /** 构造包装指定规则的实体，规则不能为 null。 */
    protected AbstractRuleEntity(T rule) {
        AssertUtil.notNull(rule, "rule should not be null");
        this.rule = rule;
    }

    /** 构造包装指定规则并携带 app、ip、port 的实体。 */
    protected AbstractRuleEntity(String app, String ip, Integer port, T rule) {
        this(rule);
        setApp(app);
        setIp(ip);
        setPort(port);
    }

    /** 设置应用名并返回当前对象。 */
    @Override
    public AbstractRuleEntity<T> setApp(String app) {
        this.app = app;
        return this;
    }

    /** 设置机器 IP 地址并返回当前对象。 */
    @Override
    public AbstractRuleEntity<T> setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** 设置机器端口并返回当前对象。 */
    @Override
    public AbstractRuleEntity<T> setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** 获取被包装的规则对象。 */
    public T getRule() {
        return rule;
    }

    /** 设置被包装的规则对象并返回当前对象。 */
    public AbstractRuleEntity<T> setRule(T rule) {
        this.rule = rule;
        return this;
    }

    /** 获取被包装规则的资源名（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public String getResource() {
        return rule.getResource();
    }

    /** 获取被包装规则的 limitApp（不参与 JSON 序列化）。 */
    @JsonIgnore
    @JSONField(serialize = false)
    public String getLimitApp() {
        return rule.getLimitApp();
    }

    /** 直接返回被包装的规则对象。 */
    @Override
    public T toRule() {
        return rule;
    }

    /** 返回实体的字符串表示。 */
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
