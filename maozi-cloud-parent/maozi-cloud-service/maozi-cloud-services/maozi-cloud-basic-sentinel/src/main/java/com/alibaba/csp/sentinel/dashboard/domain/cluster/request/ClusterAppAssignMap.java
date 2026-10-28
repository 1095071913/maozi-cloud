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
package com.alibaba.csp.sentinel.dashboard.domain.cluster.request;

import java.util.Set;

/**
 * @author Eric Zhao
 * @since 1.4.1
 */
public class ClusterAppAssignMap {

    /** The machine ID of the token server machine. */
    private String machineId;

    /** The IP address of the token server machine. */
    private String ip;

    /** The port of the token server machine. */
    private Integer port;

    /** Whether the token server machine belongs to the current app. */
    private Boolean belongToApp;

    /** The set of client machine IDs assigned to this token server. */
    private Set<String> clientSet;

    /** The set of namespaces that this token server serves. */
    private Set<String> namespaceSet;

    /** The max allowed QPS of this token server. */
    private Double maxAllowedQps;

    /** Get the machine ID. */
    public String getMachineId() {
        return machineId;
    }

    /** Set the machine ID and return this. */
    public ClusterAppAssignMap setMachineId(String machineId) {
        this.machineId = machineId;
        return this;
    }

    /** Get the IP address. */
    public String getIp() {
        return ip;
    }

    /** Set the IP address and return this. */
    public ClusterAppAssignMap setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Get the port. */
    public Integer getPort() {
        return port;
    }

    /** Set the port and return this. */
    public ClusterAppAssignMap setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** Get the set of client machine IDs. */
    public Set<String> getClientSet() {
        return clientSet;
    }

    /** Set the set of client machine IDs and return this. */
    public ClusterAppAssignMap setClientSet(Set<String> clientSet) {
        this.clientSet = clientSet;
        return this;
    }

    /** Get the set of namespaces. */
    public Set<String> getNamespaceSet() {
        return namespaceSet;
    }

    /** Set the set of namespaces and return this. */
    public ClusterAppAssignMap setNamespaceSet(Set<String> namespaceSet) {
        this.namespaceSet = namespaceSet;
        return this;
    }

    /** Get the flag whether the machine belongs to the current app. */
    public Boolean getBelongToApp() {
        return belongToApp;
    }

    /** Set the flag whether the machine belongs to the current app and return this. */
    public ClusterAppAssignMap setBelongToApp(Boolean belongToApp) {
        this.belongToApp = belongToApp;
        return this;
    }

    /** Get the max allowed QPS. */
    public Double getMaxAllowedQps() {
        return maxAllowedQps;
    }

    /** Set the max allowed QPS and return this. */
    public ClusterAppAssignMap setMaxAllowedQps(Double maxAllowedQps) {
        this.maxAllowedQps = maxAllowedQps;
        return this;
    }

    /** Return a string representation of this assignment map. */
    @Override
    public String toString() {
        return "ClusterAppAssignMap{" +
            "machineId='" + machineId + '\'' +
            ", ip='" + ip + '\'' +
            ", port=" + port +
            ", belongToApp=" + belongToApp +
            ", clientSet=" + clientSet +
            ", namespaceSet=" + namespaceSet +
            ", maxAllowedQps=" + maxAllowedQps +
            '}';
    }
}
