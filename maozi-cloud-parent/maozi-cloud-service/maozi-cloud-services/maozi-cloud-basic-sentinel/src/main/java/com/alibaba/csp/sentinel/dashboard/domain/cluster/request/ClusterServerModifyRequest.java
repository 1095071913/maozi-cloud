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
 * 修改机器集群 token server 配置的请求。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterServerModifyRequest extends AbstractClusterModifyRequest {

    /** 集群 server 的新流控配置。 */
    private ServerFlowConfig flowConfig;

    /** 集群 server 的新传输配置。 */
    private ServerTransportConfig transportConfig;

    /** 集群 server 服务的新 namespace 集合。 */
    private Set<String> namespaceSet;

    /** 获取流控配置。 */
    public ServerFlowConfig getFlowConfig() {
        return flowConfig;
    }

    /** 设置流控配置并返回当前对象。 */
    public ClusterServerModifyRequest setFlowConfig(
        ServerFlowConfig flowConfig) {
        this.flowConfig = flowConfig;
        return this;
    }

    /** 获取传输配置。 */
    public ServerTransportConfig getTransportConfig() {
        return transportConfig;
    }

    /** 设置传输配置并返回当前对象。 */
    public ClusterServerModifyRequest setTransportConfig(
        ServerTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        return this;
    }

    /** 获取 namespace 集合。 */
    public Set<String> getNamespaceSet() {
        return namespaceSet;
    }

    /** 设置 namespace 集合并返回当前对象。 */
    public ClusterServerModifyRequest setNamespaceSet(Set<String> namespaceSet) {
        this.namespaceSet = namespaceSet;
        return this;
    }

    /** 返回该请求的字符串表示。 */
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
