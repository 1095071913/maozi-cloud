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
package com.alibaba.csp.sentinel.dashboard.domain.cluster;

import java.util.HashSet;
import java.util.Set;

/**
 * 集群 token server 机器基础实体，持有 server 机器标识（machineId、ip、port）、
 * 绑定的客户端集合及是否属于当前应用的标记。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public abstract class BaseClusterServerMachine {

    /** token server 的机器 id。 */
    protected String machineId;

    /** token server 的 IP。 */
    protected String ip;

    /** token server 的端口。 */
    protected Integer port;

    /** 绑定到该 token server 的客户端机器 id 集合。 */
    protected Set<String> clientSet = new HashSet<>();

    /** token server 是否属于当前应用。 */
    protected Boolean belongToApp;

    /** 获取 token server 的机器 id。 */
    public String getMachineId() {
        return machineId;
    }

    /** 设置 token server 的机器 id 并返回当前对象。 */
    public BaseClusterServerMachine setMachineId(String machineId) {
        this.machineId = machineId;
        return this;
    }

    /** 获取 token server 的 IP。 */
    public String getIp() {
        return ip;
    }

    /** 设置 token server 的 IP 并返回当前对象。 */
    public BaseClusterServerMachine setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** 获取 token server 的端口。 */
    public Integer getPort() {
        return port;
    }

    /** 设置 token server 的端口并返回当前对象。 */
    public BaseClusterServerMachine setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** 获取绑定的客户端机器 id 集合。 */
    public Set<String> getClientSet() {
        return clientSet;
    }

    /** 设置绑定的客户端机器 id 集合并返回当前对象。 */
    public BaseClusterServerMachine setClientSet(Set<String> clientSet) {
        this.clientSet = clientSet;
        return this;
    }

    /** token server 是否属于当前应用。 */
    public Boolean getBelongToApp() {
        return belongToApp;
    }

    /** 设置 token server 是否属于当前应用并返回当前对象。 */
    public BaseClusterServerMachine setBelongToApp(Boolean belongToApp) {
        this.belongToApp = belongToApp;
        return this;
    }
}
