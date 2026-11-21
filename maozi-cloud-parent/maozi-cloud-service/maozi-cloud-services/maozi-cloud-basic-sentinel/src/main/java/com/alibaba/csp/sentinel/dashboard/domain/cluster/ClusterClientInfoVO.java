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

/**
 * 机器的集群 token client 信息，即连接的 token server 及客户端状态。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterClientInfoVO {

    /** 已连接 token server 的主机地址。 */
    private String serverHost;
    /** 已连接 token server 的端口。 */
    private Integer serverPort;

    /** 集群客户端当前状态。 */
    private Integer clientState;

    /** 集群客户端的请求超时时间。 */
    private Integer requestTimeout;

    /** 获取已连接 token server 的主机地址。 */
    public String getServerHost() {
        return serverHost;
    }

    /** 设置 token server 的主机地址并返回当前对象。 */
    public ClusterClientInfoVO setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    /** 获取已连接 token server 的端口。 */
    public Integer getServerPort() {
        return serverPort;
    }

    /** 设置 token server 的端口并返回当前对象。 */
    public ClusterClientInfoVO setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    /** 获取集群客户端当前状态。 */
    public Integer getClientState() {
        return clientState;
    }

    /** 设置集群客户端状态并返回当前对象。 */
    public ClusterClientInfoVO setClientState(Integer clientState) {
        this.clientState = clientState;
        return this;
    }

    /** 获取集群客户端的请求超时时间。 */
    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    /** 设置集群客户端的请求超时时间并返回当前对象。 */
    public ClusterClientInfoVO setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterClientInfoVO{" +
            "serverHost='" + serverHost + '\'' +
            ", serverPort=" + serverPort +
            ", clientState=" + clientState +
            ", requestTimeout=" + requestTimeout +
            '}';
    }
}
