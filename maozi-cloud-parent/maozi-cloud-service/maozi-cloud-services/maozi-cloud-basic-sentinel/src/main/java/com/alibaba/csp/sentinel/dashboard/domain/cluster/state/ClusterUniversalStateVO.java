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
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ClusterUniversalStateVO {

    /** The simple cluster state info of the machine. */
    private ClusterStateSimpleEntity stateInfo;

    /** The cluster client state of the machine. */
    private ClusterClientStateVO client;

    /** The cluster server state of the machine. */
    private ClusterServerStateVO server;

    /** Get the cluster client state. */
    public ClusterClientStateVO getClient() {
        return client;
    }

    /** Set the cluster client state and return this. */
    public ClusterUniversalStateVO setClient(ClusterClientStateVO client) {
        this.client = client;
        return this;
    }

    /** Get the cluster server state. */
    public ClusterServerStateVO getServer() {
        return server;
    }

    /** Set the cluster server state and return this. */
    public ClusterUniversalStateVO setServer(ClusterServerStateVO server) {
        this.server = server;
        return this;
    }

    /** Get the simple cluster state info. */
    public ClusterStateSimpleEntity getStateInfo() {
        return stateInfo;
    }

    /** Set the simple cluster state info and return this. */
    public ClusterUniversalStateVO setStateInfo(
        ClusterStateSimpleEntity stateInfo) {
        this.stateInfo = stateInfo;
        return this;
    }

    /** Return a string representation of this VO. */
    @Override
    public String toString() {
        return "ClusterUniversalStateVO{" +
            "stateInfo=" + stateInfo +
            ", client=" + client +
            ", server=" + server +
            '}';
    }
}
