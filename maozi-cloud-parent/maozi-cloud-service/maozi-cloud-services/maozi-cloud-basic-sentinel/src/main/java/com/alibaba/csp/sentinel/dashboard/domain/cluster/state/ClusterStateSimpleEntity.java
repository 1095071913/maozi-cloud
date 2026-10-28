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
public class ClusterStateSimpleEntity {

    /** The current cluster mode of the machine. */
    private Integer mode;

    /** The timestamp of the last modification of the cluster state. */
    private Long lastModified;

    /** Whether the cluster client is available. */
    private Boolean clientAvailable;

    /** Whether the cluster server is available. */
    private Boolean serverAvailable;

    /** Get the cluster mode. */
    public Integer getMode() {
        return mode;
    }

    /** Set the cluster mode and return this. */
    public ClusterStateSimpleEntity setMode(Integer mode) {
        this.mode = mode;
        return this;
    }

    /** Get the last modified timestamp. */
    public Long getLastModified() {
        return lastModified;
    }

    /** Set the last modified timestamp and return this. */
    public ClusterStateSimpleEntity setLastModified(Long lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    /** Get the flag whether the cluster client is available. */
    public Boolean getClientAvailable() {
        return clientAvailable;
    }

    /** Set the flag whether the cluster client is available and return this. */
    public ClusterStateSimpleEntity setClientAvailable(Boolean clientAvailable) {
        this.clientAvailable = clientAvailable;
        return this;
    }

    /** Get the flag whether the cluster server is available. */
    public Boolean getServerAvailable() {
        return serverAvailable;
    }

    /** Set the flag whether the cluster server is available and return this. */
    public ClusterStateSimpleEntity setServerAvailable(Boolean serverAvailable) {
        this.serverAvailable = serverAvailable;
        return this;
    }

    /** Return a string representation of this entity. */
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
