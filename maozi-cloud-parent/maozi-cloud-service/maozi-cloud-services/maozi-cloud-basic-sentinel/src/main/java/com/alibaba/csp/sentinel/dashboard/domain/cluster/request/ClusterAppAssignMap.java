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

import com.alibaba.csp.sentinel.dashboard.domain.cluster.BaseClusterServerMachine;

import java.util.Set;

/**
 * Assignment of one cluster token server: the server machine with the clients, namespaces and max QPS bound to it.
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterAppAssignMap extends BaseClusterServerMachine {

    /** The set of namespaces that this token server serves. */
    private Set<String> namespaceSet;

    /** The max allowed QPS of this token server. */
    private Double maxAllowedQps;

    /** Get the set of namespaces. */
    public Set<String> getNamespaceSet() {
        return namespaceSet;
    }

    /** Set the set of namespaces and return this. */
    public ClusterAppAssignMap setNamespaceSet(Set<String> namespaceSet) {
        this.namespaceSet = namespaceSet;
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
