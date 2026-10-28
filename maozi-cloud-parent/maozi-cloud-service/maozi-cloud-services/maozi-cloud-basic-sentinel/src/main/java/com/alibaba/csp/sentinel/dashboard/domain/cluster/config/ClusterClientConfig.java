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
 * @author Eric Zhao
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

    /** Get the host of the token server that the client connects to. */
    public String getServerHost() {
        return serverHost;
    }

    /** Set the host of the token server and return this. */
    public ClusterClientConfig setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    /** Get the port of the token server that the client connects to. */
    public Integer getServerPort() {
        return serverPort;
    }

    /** Set the port of the token server and return this. */
    public ClusterClientConfig setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    /** Get the request timeout of the cluster client. */
    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    /** Set the request timeout of the cluster client and return this. */
    public ClusterClientConfig setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /** Get the connect timeout of the cluster client. */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /** Set the connect timeout of the cluster client and return this. */
    public ClusterClientConfig setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /** Get the string representation of this config. */
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
