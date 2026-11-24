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

import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterAppAssignMap;

import java.util.Set;

/**
 * 将一组客户端绑定到单个集群 token server 的分配请求。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterAppSingleServerAssignRequest {

    /** 单个 token server 的分配信息，含其绑定的客户端。 */
    private ClusterAppAssignMap clusterMap;
    /** 未分配到该 server 的机器 id 集合。 */
    private Set<String> remainingList;

    /** 获取单个 token server 的分配信息。 */
    public ClusterAppAssignMap getClusterMap() {
        return clusterMap;
    }

    /** 设置 token server 的分配信息并返回当前对象。 */
    public ClusterAppSingleServerAssignRequest setClusterMap(ClusterAppAssignMap clusterMap) {
        this.clusterMap = clusterMap;
        return this;
    }

    /** 获取仍未分配的机器 id 集合。 */
    public Set<String> getRemainingList() {
        return remainingList;
    }

    /** 设置仍未分配的机器 id 集合并返回当前对象。 */
    public ClusterAppSingleServerAssignRequest setRemainingList(Set<String> remainingList) {
        this.remainingList = remainingList;
        return this;
    }

    /** 返回该请求的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterAppSingleServerAssignRequest{"
                + "clusterMap=" + clusterMap
                + ", remainingList=" + remainingList
                + '}';
    }
}
