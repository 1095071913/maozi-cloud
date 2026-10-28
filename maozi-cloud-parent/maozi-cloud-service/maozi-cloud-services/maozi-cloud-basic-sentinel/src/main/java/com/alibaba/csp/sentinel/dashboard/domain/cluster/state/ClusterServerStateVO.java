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

import com.alibaba.csp.sentinel.dashboard.domain.cluster.ConnectionGroupVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerFlowConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerTransportConfig;

import java.util.List;
import java.util.Set;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ClusterServerStateVO {

    /** The app name that this token server belongs to. */
    private String appName;

    /** The transport config of the token server. */
    private ServerTransportConfig transport;

    /** The flow config of the token server. */
    private ServerFlowConfig flow;

    /** The set of namespaces that this token server serves. */
    private Set<String> namespaceSet;

    /** The port of the token server. */
    private Integer port;

    /** The list of connection groups of clients connected to this token server. */
    private List<ConnectionGroupVO> connection;

    /** The request limit data of each namespace. */
    private List<ClusterRequestLimitVO> requestLimitData;

    /** Whether the token server works in embedded mode. */
    private Boolean embedded;

    /** Get the app name. */
    public String getAppName() {
        return appName;
    }

    /** Set the app name and return this. */
    public ClusterServerStateVO setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    /** Get the transport config. */
    public ServerTransportConfig getTransport() {
        return transport;
    }

    /** Set the transport config and return this. */
    public ClusterServerStateVO setTransport(ServerTransportConfig transport) {
        this.transport = transport;
        return this;
    }

    /** Get the flow config. */
    public ServerFlowConfig getFlow() {
        return flow;
    }

    /** Set the flow config and return this. */
    public ClusterServerStateVO setFlow(ServerFlowConfig flow) {
        this.flow = flow;
        return this;
    }

    /** Get the set of namespaces. */
    public Set<String> getNamespaceSet() {
        return namespaceSet;
    }

    /** Set the set of namespaces and return this. */
    public ClusterServerStateVO setNamespaceSet(Set<String> namespaceSet) {
        this.namespaceSet = namespaceSet;
        return this;
    }

    /** Get the port. */
    public Integer getPort() {
        return port;
    }

    /** Set the port and return this. */
    public ClusterServerStateVO setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** Get the list of connection groups. */
    public List<ConnectionGroupVO> getConnection() {
        return connection;
    }

    /** Set the list of connection groups and return this. */
    public ClusterServerStateVO setConnection(List<ConnectionGroupVO> connection) {
        this.connection = connection;
        return this;
    }

    /** Get the request limit data of each namespace. */
    public List<ClusterRequestLimitVO> getRequestLimitData() {
        return requestLimitData;
    }

    /** Set the request limit data of each namespace and return this. */
    public ClusterServerStateVO setRequestLimitData(List<ClusterRequestLimitVO> requestLimitData) {
        this.requestLimitData = requestLimitData;
        return this;
    }

    /** Get the flag whether the token server works in embedded mode. */
    public Boolean getEmbedded() {
        return embedded;
    }

    /** Set the flag whether the token server works in embedded mode and return this. */
    public ClusterServerStateVO setEmbedded(Boolean embedded) {
        this.embedded = embedded;
        return this;
    }

    /** Return a string representation of this VO. */
    @Override
    public String toString() {
        return "ClusterServerStateVO{" +
            "appName='" + appName + '\'' +
            ", transport=" + transport +
            ", flow=" + flow +
            ", namespaceSet=" + namespaceSet +
            ", port=" + port +
            ", connection=" + connection +
            ", requestLimitData=" + requestLimitData +
            ", embedded=" + embedded +
            '}';
    }
}
