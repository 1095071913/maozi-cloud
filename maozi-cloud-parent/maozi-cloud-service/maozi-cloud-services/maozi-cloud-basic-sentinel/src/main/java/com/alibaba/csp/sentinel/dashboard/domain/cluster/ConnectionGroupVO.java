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

import java.util.List;

/**
 * 到集群 token server 的连接分组，按 namespace 归组。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ConnectionGroupVO {

    /** 连接分组的 namespace。 */
    private String namespace;
    /** 分组内的连接列表。 */
    private List<ConnectionDescriptorVO> connectionSet;
    /** 已连接的客户端数量。 */
    private Integer connectedCount;

    /** 获取连接分组的 namespace。 */
    public String getNamespace() {
        return namespace;
    }

    /** 设置连接分组的 namespace 并返回当前对象。 */
    public ConnectionGroupVO setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /** 获取分组内的连接列表。 */
    public List<ConnectionDescriptorVO> getConnectionSet() {
        return connectionSet;
    }

    /** 设置分组内的连接列表并返回当前对象。 */
    public ConnectionGroupVO setConnectionSet(
        List<ConnectionDescriptorVO> connectionSet) {
        this.connectionSet = connectionSet;
        return this;
    }

    /** 获取已连接的客户端数量。 */
    public Integer getConnectedCount() {
        return connectedCount;
    }

    /** 设置已连接的客户端数量并返回当前对象。 */
    public ConnectionGroupVO setConnectedCount(Integer connectedCount) {
        this.connectedCount = connectedCount;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ConnectionGroupVO{" +
            "namespace='" + namespace + '\'' +
            ", connectionSet=" + connectionSet +
            ", connectedCount=" + connectedCount +
            '}';
    }
}
