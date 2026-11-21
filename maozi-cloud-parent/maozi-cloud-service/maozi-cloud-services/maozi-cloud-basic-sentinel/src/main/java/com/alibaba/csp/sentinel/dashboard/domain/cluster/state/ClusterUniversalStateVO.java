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

/**
 * 机器的通用集群状态：简要状态及可选的客户端、服务端状态。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterUniversalStateVO {

    /** 机器的简要集群状态信息。 */
    private ClusterStateSimpleEntity stateInfo;

    /** 机器的集群客户端状态。 */
    private ClusterClientStateVO client;

    /** 机器的集群服务端状态。 */
    private ClusterServerStateVO server;

    /** 获取集群客户端状态。 */
    public ClusterClientStateVO getClient() {
        return client;
    }

    /** 设置集群客户端状态并返回当前对象。 */
    public ClusterUniversalStateVO setClient(ClusterClientStateVO client) {
        this.client = client;
        return this;
    }

    /** 获取集群服务端状态。 */
    public ClusterServerStateVO getServer() {
        return server;
    }

    /** 设置集群服务端状态并返回当前对象。 */
    public ClusterUniversalStateVO setServer(ClusterServerStateVO server) {
        this.server = server;
        return this;
    }

    /** 获取简要集群状态信息。 */
    public ClusterStateSimpleEntity getStateInfo() {
        return stateInfo;
    }

    /** 设置简要集群状态信息并返回当前对象。 */
    public ClusterUniversalStateVO setStateInfo(
        ClusterStateSimpleEntity stateInfo) {
        this.stateInfo = stateInfo;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterUniversalStateVO{" +
            "stateInfo=" + stateInfo +
            ", client=" + client +
            ", server=" + server +
            '}';
    }
}
