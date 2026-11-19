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

import java.util.Date;

/**
 * Base entity holding a wrapped {@link AbstractRule} plus common fields shared by rule entities.
 *
 * @author Eric Zhao
 * @since 0.2.1
 */
public abstract class AbstractRuleEntity<T extends AbstractRule> implements RuleEntity {

    /** The entity id. */
    protected Long id;

    /** The application name. */
    protected String app;
    /** The IP address of the machine. */
    protected String ip;
    /** The port of the machine. */
    protected Integer port;

    /** The wrapped rule object. */
    protected T rule;

    /** The create time. */
    private Date gmtCreate;
    /** The last modify time. */
    private Date gmtModified;

    /** Gets the entity id. */
    @Override
    public Long getId() {
        return id;
    }

    /** Sets the entity id. */
    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /** Gets the application name. */
    @Override
    public String getApp() {
        return app;
    }

    /** Sets the application name and returns this. */
    public AbstractRuleEntity<T> setApp(String app) {
        this.app = app;
        return this;
    }

    /** Gets the IP address of the machine. */
    @Override
    public String getIp() {
        return ip;
    }

    /** Sets the IP address of the machine and returns this. */
    public AbstractRuleEntity<T> setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Gets the port of the machine. */
    @Override
    public Integer getPort() {
        return port;
    }

    /** Sets the port of the machine and returns this. */
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

    /** Gets the create time. */
    @Override
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /** Sets the create time and returns this. */
    public AbstractRuleEntity<T> setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
        return this;
    }

    /** Gets the last modify time. */
    public Date getGmtModified() {
        return gmtModified;
    }

    /** Sets the last modify time and returns this. */
    public AbstractRuleEntity<T> setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
        return this;
    }

    /** Returns the wrapped rule object directly. */
    @Override
    public T toRule() {
        return rule;
    }
}
