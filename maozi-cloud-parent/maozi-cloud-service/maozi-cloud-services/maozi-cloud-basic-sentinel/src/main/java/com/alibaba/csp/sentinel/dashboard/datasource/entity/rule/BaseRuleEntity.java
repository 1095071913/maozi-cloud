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
 * 规则实体基类，持有各规则实体的公共字段（id、app、ip、port、gmtCreate、gmtModified），
 * 子类只需携带自身规则字段并实现 {@link #toRule()}。
 *
 * @author leyou
 * @date 2026/11/22
 */
public abstract class BaseRuleEntity extends BaseEntity implements RuleEntity {

    /** 机器 IP 地址。 */
    protected String ip;
    /** 机器端口。 */
    protected Integer port;

    /** 获取机器 IP 地址。 */
    @Override
    public String getIp() {
        return ip;
    }

    /** 设置机器 IP 地址并返回当前对象。 */
    public BaseRuleEntity setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** 获取机器端口。 */
    @Override
    public Integer getPort() {
        return port;
    }

    /** 设置机器端口并返回当前对象。 */
    public BaseRuleEntity setPort(Integer port) {
        this.port = port;
        return this;
    }
}
