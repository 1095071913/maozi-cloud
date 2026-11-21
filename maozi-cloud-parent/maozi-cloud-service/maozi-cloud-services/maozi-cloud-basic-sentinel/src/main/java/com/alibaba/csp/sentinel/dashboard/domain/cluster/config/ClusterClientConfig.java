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
 * Cluster token client config: the target server address and the request/connect timeouts.
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterClientConfig {

    /** The host of the token server that the client connects to. */
    private String serverHost;
    /** The port of the token server that the client connects to. */
    private Integer serverPort;

    /** The request timeout of the cluster client. */
    private Integer requestTimeout;
    /** The connect timeout of the cluster client. */
    private Integer connectTimeout;

    /** Gets the host of the token server that the client connects to. */
    public String getServerHost() {
        return serverHost;
    }

    /** Sets the host of the token server and returns this. */
    public ClusterClientConfig setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    /** Gets the port of the token server that the client connects to. */
    public Integer getServerPort() {
        return serverPort;
    }

    /** Sets the port of the token server and returns this. */
    public ClusterClientConfig setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    /** Gets the request timeout of the cluster client. */
    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    /** Sets the request timeout of the cluster client and returns this. */
    public ClusterClientConfig setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /** Gets the connect timeout of the cluster client. */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /** Sets the connect timeout of the cluster client and returns this. */
    public ClusterClientConfig setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /** Gets the string representation of this config. */
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
