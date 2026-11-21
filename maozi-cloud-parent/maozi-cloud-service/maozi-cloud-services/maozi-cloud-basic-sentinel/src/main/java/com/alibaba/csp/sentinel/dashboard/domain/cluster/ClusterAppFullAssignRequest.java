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

import java.util.List;
import java.util.Set;

/**
 * 应用整体集群分配请求，携带完整的 server-client 映射及剩余未分配的机器。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterAppFullAssignRequest {

    /** token server 分配列表，含各 server 绑定的客户端。 */
    private List<ClusterAppAssignMap> clusterMap;
    /** 集群映射中仍未分配的机器 id 集合。 */
    private Set<String> remainingList;

    /** 获取 token server 分配列表。 */
    public List<ClusterAppAssignMap> getClusterMap() {
        return clusterMap;
    }

    /** 设置 token server 分配列表并返回当前对象。 */
    public ClusterAppFullAssignRequest setClusterMap(
        List<ClusterAppAssignMap> clusterMap) {
        this.clusterMap = clusterMap;
        return this;
    }

    /** 获取仍未分配的机器 id 集合。 */
    public Set<String> getRemainingList() {
        return remainingList;
    }

    /** 设置仍未分配的机器 id 集合并返回当前对象。 */
    public ClusterAppFullAssignRequest setRemainingList(Set<String> remainingList) {
        this.remainingList = remainingList;
        return this;
    }

    /** 返回该请求的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterAppFullAssignRequest{" +
            "clusterMap=" + clusterMap +
            ", remainingList=" + remainingList +
            '}';
    }
}
