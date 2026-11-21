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
 * 机器的简要集群状态：模式、客户端/服务端可用性及最后修改时间。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterStateSimpleEntity {

    /** 机器当前集群模式。 */
    private Integer mode;

    /** 集群状态最近一次修改的时间戳。 */
    private Long lastModified;

    /** 集群客户端是否可用。 */
    private Boolean clientAvailable;

    /** 集群服务端是否可用。 */
    private Boolean serverAvailable;

    /** 获取集群模式。 */
    public Integer getMode() {
        return mode;
    }

    /** 设置集群模式并返回当前对象。 */
    public ClusterStateSimpleEntity setMode(Integer mode) {
        this.mode = mode;
        return this;
    }

    /** 获取最近一次修改的时间戳。 */
    public Long getLastModified() {
        return lastModified;
    }

    /** 设置最近一次修改的时间戳并返回当前对象。 */
    public ClusterStateSimpleEntity setLastModified(Long lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    /** 获取集群客户端是否可用的标记。 */
    public Boolean getClientAvailable() {
        return clientAvailable;
    }

    /** 设置集群客户端是否可用的标记并返回当前对象。 */
    public ClusterStateSimpleEntity setClientAvailable(Boolean clientAvailable) {
        this.clientAvailable = clientAvailable;
        return this;
    }

    /** 获取集群服务端是否可用的标记。 */
    public Boolean getServerAvailable() {
        return serverAvailable;
    }

    /** 设置集群服务端是否可用的标记并返回当前对象。 */
    public ClusterStateSimpleEntity setServerAvailable(Boolean serverAvailable) {
        this.serverAvailable = serverAvailable;
        return this;
    }

    /** 返回该实体的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterStateSimpleEntity{" +
            "mode=" + mode +
            ", lastModified=" + lastModified +
            ", clientAvailable=" + clientAvailable +
            ", serverAvailable=" + serverAvailable +
            '}';
    }
}
