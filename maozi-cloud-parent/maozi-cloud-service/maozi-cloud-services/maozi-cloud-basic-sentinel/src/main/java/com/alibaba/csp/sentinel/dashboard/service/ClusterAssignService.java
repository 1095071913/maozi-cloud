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
package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterAppAssignResultVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterAppAssignMap;

import java.util.List;
import java.util.Set;

/**
 * 应用集群 Token 服务端与客户端的分配及解绑服务
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public interface ClusterAssignService {

    /**
     * 解绑指定的集群服务端及其客户端
     *
     * @param app 应用名
     * @param machineId 有效的机器 ID（{@code host@commandPort}）
     * @return 分配结果
     */
    ClusterAppAssignResultVO unbindClusterServer(String app, String machineId);

    /**
     * 解绑一组集群服务端及其客户端
     *
     * @param app 应用名
     * @param machineIdSet 有效机器 ID（{@code host@commandPort}）集合
     * @return 分配结果
     */
    ClusterAppAssignResultVO unbindClusterServers(String app, Set<String> machineIdSet);

    /**
     * 为指定应用执行集群服务端与客户端的分配
     *
     * @param app 应用名
     * @param clusterMap 集群分配映射（服务端 -> 客户端）
     * @param remainingSet 未分配的机器 ID 集合
     * @return 分配结果
     */
    ClusterAppAssignResultVO applyAssignToApp(String app, List<ClusterAppAssignMap> clusterMap,
            Set<String> remainingSet);
}
