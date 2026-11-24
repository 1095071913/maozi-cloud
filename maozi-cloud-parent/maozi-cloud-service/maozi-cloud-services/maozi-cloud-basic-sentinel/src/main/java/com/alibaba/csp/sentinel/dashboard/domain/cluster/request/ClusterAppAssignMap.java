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
 * 单个集群 token server 的分配信息：server 机器及其绑定的客户端、namespace 和最大 QPS。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterAppAssignMap extends BaseClusterServerMachine {

    /** 该 token server 服务的 namespace 集合。 */
    private Set<String> namespaceSet;

    /** 该 token server 的最大允许 QPS。 */
    private Double maxAllowedQps;

    /** 获取 namespace 集合。 */
    public Set<String> getNamespaceSet() {
        return namespaceSet;
    }

    /** 设置 namespace 集合并返回当前对象。 */
    public ClusterAppAssignMap setNamespaceSet(Set<String> namespaceSet) {
        this.namespaceSet = namespaceSet;
        return this;
    }

    /** 获取最大允许 QPS。 */
    public Double getMaxAllowedQps() {
        return maxAllowedQps;
    }

    /** 设置最大允许 QPS 并返回当前对象。 */
    public ClusterAppAssignMap setMaxAllowedQps(Double maxAllowedQps) {
        this.maxAllowedQps = maxAllowedQps;
        return this;
    }

    /** 返回该分配信息的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterAppAssignMap{"
                + "machineId='" + machineId + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", belongToApp=" + belongToApp
                + ", clientSet=" + clientSet
                + ", namespaceSet=" + namespaceSet
                + ", maxAllowedQps=" + maxAllowedQps
                + '}';
    }
}
