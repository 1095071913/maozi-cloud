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

import com.alibaba.csp.sentinel.dashboard.datasource.entity.BaseEntity;

/**
 * Base entity of rule entities, holding the common fields shared by rule entities
 * (id, app, ip, port, gmtCreate and gmtModified). Subclasses only need to carry
 * their own rule fields and implement {@link #toRule()}.
 *
 * @author leyou
 */
public abstract class BaseRuleEntity extends BaseEntity implements RuleEntity {

    /** The IP address of the machine. */
    protected String ip;
    /** The port of the machine. */
    protected Integer port;

    /** Gets the IP address of the machine. */
    @Override
    public String getIp() {
        return ip;
    }

    /** Sets the IP address of the machine and returns this. */
    public BaseRuleEntity setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Gets the port of the machine. */
    @Override
    public Integer getPort() {
        return port;
    }

    /** Sets the port of the machine and returns this. */
    public BaseRuleEntity setPort(Integer port) {
        this.port = port;
        return this;
    }
}
