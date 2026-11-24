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
 * 应用级单台机器的集群 token client 状态包装，按机器 id 标识。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class AppClusterClientStateWrapVO {

    /** 机器 id，格式为 "{ip}@{transport_command_port}"。 */
    private String id;

    /** 机器命令端口。 */
    private Integer commandPort;

    /** 机器 IP 地址。 */
    private String ip;

    /** 机器的集群客户端状态。 */
    private ClusterClientStateVO state;

    /** 获取机器 id。 */
    public String getId() {
        return id;
    }

    /** 设置机器 id 并返回当前对象。 */
    public AppClusterClientStateWrapVO setId(String id) {
        this.id = id;
        return this;
    }

    /** 获取 IP 地址。 */
    public String getIp() {
        return ip;
    }

    /** 设置 IP 地址并返回当前对象。 */
    public AppClusterClientStateWrapVO setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** 获取集群客户端状态。 */
    public ClusterClientStateVO getState() {
        return state;
    }

    /** 设置集群客户端状态并返回当前对象。 */
    public AppClusterClientStateWrapVO setState(ClusterClientStateVO state) {
        this.state = state;
        return this;
    }

    /** 获取命令端口。 */
    public Integer getCommandPort() {
        return commandPort;
    }

    /** 设置命令端口并返回当前对象。 */
    public AppClusterClientStateWrapVO setCommandPort(Integer commandPort) {
        this.commandPort = commandPort;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "AppClusterClientStateWrapVO{"
                + "id='" + id + '\''
                + ", commandPort=" + commandPort
                + ", ip='" + ip + '\''
                + ", state=" + state
                + '}';
    }
}
