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
 * 机器的集群 token server 状态。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterServerStateVO {

    /** 该 token server 所属应用名。 */
    private String appName;

    /** token server 的传输配置。 */
    private ServerTransportConfig transport;

    /** token server 的流控配置。 */
    private ServerFlowConfig flow;

    /** 该 token server 服务的 namespace 集合。 */
    private Set<String> namespaceSet;

    /** token server 的端口。 */
    private Integer port;

    /** 连接到该 token server 的客户端连接分组列表。 */
    private List<ConnectionGroupVO> connection;

    /** 各 namespace 的请求限流数据。 */
    private List<ClusterRequestLimitVO> requestLimitData;

    /** token server 是否以嵌入模式运行。 */
    private Boolean embedded;

    /** 获取应用名。 */
    public String getAppName() {
        return appName;
    }

    /** 设置应用名并返回当前对象。 */
    public ClusterServerStateVO setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    /** 获取传输配置。 */
    public ServerTransportConfig getTransport() {
        return transport;
    }

    /** 设置传输配置并返回当前对象。 */
    public ClusterServerStateVO setTransport(ServerTransportConfig transport) {
        this.transport = transport;
        return this;
    }

    /** 获取流控配置。 */
    public ServerFlowConfig getFlow() {
        return flow;
    }

    /** 设置流控配置并返回当前对象。 */
    public ClusterServerStateVO setFlow(ServerFlowConfig flow) {
        this.flow = flow;
        return this;
    }

    /** 获取 namespace 集合。 */
    public Set<String> getNamespaceSet() {
        return namespaceSet;
    }

    /** 设置 namespace 集合并返回当前对象。 */
    public ClusterServerStateVO setNamespaceSet(Set<String> namespaceSet) {
        this.namespaceSet = namespaceSet;
        return this;
    }

    /** 获取端口。 */
    public Integer getPort() {
        return port;
    }

    /** 设置端口并返回当前对象。 */
    public ClusterServerStateVO setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** 获取连接分组列表。 */
    public List<ConnectionGroupVO> getConnection() {
        return connection;
    }

    /** 设置连接分组列表并返回当前对象。 */
    public ClusterServerStateVO setConnection(List<ConnectionGroupVO> connection) {
        this.connection = connection;
        return this;
    }

    /** 获取各 namespace 的请求限流数据。 */
    public List<ClusterRequestLimitVO> getRequestLimitData() {
        return requestLimitData;
    }

    /** 设置各 namespace 的请求限流数据并返回当前对象。 */
    public ClusterServerStateVO setRequestLimitData(List<ClusterRequestLimitVO> requestLimitData) {
        this.requestLimitData = requestLimitData;
        return this;
    }

    /** 获取 token server 是否以嵌入模式运行的标记。 */
    public Boolean getEmbedded() {
        return embedded;
    }

    /** 设置 token server 是否以嵌入模式运行的标记并返回当前对象。 */
    public ClusterServerStateVO setEmbedded(Boolean embedded) {
        this.embedded = embedded;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
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
