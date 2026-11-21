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
package com.alibaba.csp.sentinel.dashboard.domain.cluster.config;

/**
 * 集群 token client 配置：目标 server 地址及请求、连接超时时间。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterClientConfig {

    /** 客户端连接的 token server 主机地址。 */
    private String serverHost;
    /** 客户端连接的 token server 端口。 */
    private Integer serverPort;

    /** 集群客户端的请求超时时间。 */
    private Integer requestTimeout;
    /** 集群客户端的连接超时时间。 */
    private Integer connectTimeout;

    /** 获取客户端连接的 token server 主机地址。 */
    public String getServerHost() {
        return serverHost;
    }

    /** 设置 token server 主机地址并返回当前对象。 */
    public ClusterClientConfig setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    /** 获取客户端连接的 token server 端口。 */
    public Integer getServerPort() {
        return serverPort;
    }

    /** 设置 token server 端口并返回当前对象。 */
    public ClusterClientConfig setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    /** 获取集群客户端的请求超时时间。 */
    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    /** 设置集群客户端的请求超时时间并返回当前对象。 */
    public ClusterClientConfig setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /** 获取集群客户端的连接超时时间。 */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /** 设置集群客户端的连接超时时间并返回当前对象。 */
    public ClusterClientConfig setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /** 返回该配置的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterClientConfig{" +
            "serverHost='" + serverHost + '\'' +
            ", serverPort=" + serverPort +
            ", requestTimeout=" + requestTimeout +
            ", connectTimeout=" + connectTimeout +
            '}';
    }
}
