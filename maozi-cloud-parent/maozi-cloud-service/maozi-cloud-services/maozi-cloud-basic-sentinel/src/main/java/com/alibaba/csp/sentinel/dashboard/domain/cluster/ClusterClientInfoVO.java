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
 * @author Eric Zhao
 * @since 1.4.1
 */
public class ClusterClientInfoVO {

    /** The host of the connected token server. */
    private String serverHost;
    /** The port of the connected token server. */
    private Integer serverPort;

    /** The current state of the cluster client. */
    private Integer clientState;

    /** The request timeout of the cluster client. */
    private Integer requestTimeout;

    /** Get the host of the connected token server. */
    public String getServerHost() {
        return serverHost;
    }

    /** Set the host of the token server and return this. */
    public ClusterClientInfoVO setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    /** Get the port of the connected token server. */
    public Integer getServerPort() {
        return serverPort;
    }

    /** Set the port of the token server and return this. */
    public ClusterClientInfoVO setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    /** Get the current state of the cluster client. */
    public Integer getClientState() {
        return clientState;
    }

    /** Set the state of the cluster client and return this. */
    public ClusterClientInfoVO setClientState(Integer clientState) {
        this.clientState = clientState;
        return this;
    }

    /** Get the request timeout of the cluster client. */
    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    /** Set the request timeout of the cluster client and return this. */
    public ClusterClientInfoVO setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /** Get the string representation of this VO. */
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
