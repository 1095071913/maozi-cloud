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

import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerFlowConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerTransportConfig;

import java.util.Set;

/**
 * Request to modify the cluster token server config of a machine.
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterServerModifyRequest extends AbstractClusterModifyRequest {

    /** The new flow config of the cluster server. */
    private ServerFlowConfig flowConfig;

    /** The new transport config of the cluster server. */
    private ServerTransportConfig transportConfig;

    /** The new set of namespaces served by the cluster server. */
    private Set<String> namespaceSet;

    /** Get the flow config. */
    public ServerFlowConfig getFlowConfig() {
        return flowConfig;
    }

    /** Set the flow config and return this. */
    public ClusterServerModifyRequest setFlowConfig(
        ServerFlowConfig flowConfig) {
        this.flowConfig = flowConfig;
        return this;
    }

    /** Get the transport config. */
    public ServerTransportConfig getTransportConfig() {
        return transportConfig;
    }

    /** Set the transport config and return this. */
    public ClusterServerModifyRequest setTransportConfig(
        ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        return this;
    }

    /** Get the set of namespaces. */
    public Set<String> getNamespaceSet() {
        return namespaceSet;
    }

    /** Set the set of namespaces and return this. */
    public ClusterServerModifyRequest setNamespaceSet(Set<String> namespaceSet) {
        this.namespaceSet = namespaceSet;
        return this;
    }

    /** Return a string representation of this request. */
    @Override
    public String toString() {
        return "ClusterServerModifyRequest{" +
            "app='" + app + '\'' +
            ", ip='" + ip + '\'' +
            ", port=" + port +
            ", mode=" + mode +
            ", flowConfig=" + flowConfig +
            ", transportConfig=" + transportConfig +
            ", namespaceSet=" + namespaceSet +
            '}';
    }
}
