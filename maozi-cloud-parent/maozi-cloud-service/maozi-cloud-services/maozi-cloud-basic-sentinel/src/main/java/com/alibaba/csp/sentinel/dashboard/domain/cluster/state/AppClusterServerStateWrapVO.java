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
package com.alibaba.csp.sentinel.dashboard.domain.cluster.state;

/**
 * 应用级单台机器的集群 token server 状态包装，按机器 id 标识。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class AppClusterServerStateWrapVO {

    /** 机器 id，格式为 "{ip}@{transport_command_port}"。 */
    private String id;

    /** token server 机器的 IP 地址。 */
    private String ip;

    /** token server 机器的命令端口。 */
    private Integer port;

    /** 连接到该 token server 的客户端数量。 */
    private Integer connectedCount;

    /** token server 机器是否属于当前应用。 */
    private Boolean belongToApp;

    /** 机器的集群服务端状态。 */
    private ClusterServerStateVO state;

    /** 获取机器 id。 */
    public String getId() {
        return id;
    }

    /** 设置机器 id 并返回当前对象。 */
    public AppClusterServerStateWrapVO setId(String id) {
        this.id = id;
        return this;
    }

    /** 获取 IP 地址。 */
    public String getIp() {
        return ip;
    }

    /** 设置 IP 地址并返回当前对象。 */
    public AppClusterServerStateWrapVO setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** 获取命令端口。 */
    public Integer getPort() {
        return port;
    }

    /** 设置命令端口并返回当前对象。 */
    public AppClusterServerStateWrapVO setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** 获取机器是否属于当前应用的标记。 */
    public Boolean getBelongToApp() {
        return belongToApp;
    }

    /** 设置机器是否属于当前应用的标记并返回当前对象。 */
    public AppClusterServerStateWrapVO setBelongToApp(Boolean belongToApp) {
        this.belongToApp = belongToApp;
        return this;
    }

    /** 获取已连接的客户端数量。 */
    public Integer getConnectedCount() {
        return connectedCount;
    }

    /** 设置已连接的客户端数量并返回当前对象。 */
    public AppClusterServerStateWrapVO setConnectedCount(Integer connectedCount) {
        this.connectedCount = connectedCount;
        return this;
    }

    /** 获取集群服务端状态。 */
    public ClusterServerStateVO getState() {
        return state;
    }

    /** 设置集群服务端状态并返回当前对象。 */
    public AppClusterServerStateWrapVO setState(ClusterServerStateVO state) {
        this.state = state;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "AppClusterServerStateWrapVO{" +
            "id='" + id + '\'' +
            ", ip='" + ip + '\'' +
            ", port='" + port + '\'' +
            ", belongToApp=" + belongToApp +
            ", state=" + state +
            '}';
    }
}
