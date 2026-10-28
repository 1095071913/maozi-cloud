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
 * @since 1.4.1
 */
public class AppClusterServerStateWrapVO {

    /**
     * {ip}@{transport_command_port}.
     */
    private String id;

    /** The IP address of the token server machine. */
    private String ip;

    /** The command port of the token server machine. */
    private Integer port;

    /** The count of clients connected to this token server. */
    private Integer connectedCount;

    /** Whether the token server machine belongs to the current app. */
    private Boolean belongToApp;

    /** The cluster server state of the machine. */
    private ClusterServerStateVO state;

    /** Get the machine ID. */
    public String getId() {
        return id;
    }

    /** Set the machine ID and return this. */
    public AppClusterServerStateWrapVO setId(String id) {
        this.id = id;
        return this;
    }

    /** Get the IP address. */
    public String getIp() {
        return ip;
    }

    /** Set the IP address and return this. */
    public AppClusterServerStateWrapVO setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Get the command port. */
    public Integer getPort() {
        return port;
    }

    /** Set the command port and return this. */
    public AppClusterServerStateWrapVO setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** Get the flag whether the machine belongs to the current app. */
    public Boolean getBelongToApp() {
        return belongToApp;
    }

    /** Set the flag whether the machine belongs to the current app and return this. */
    public AppClusterServerStateWrapVO setBelongToApp(Boolean belongToApp) {
        this.belongToApp = belongToApp;
        return this;
    }

    /** Get the count of connected clients. */
    public Integer getConnectedCount() {
        return connectedCount;
    }

    /** Set the count of connected clients and return this. */
    public AppClusterServerStateWrapVO setConnectedCount(Integer connectedCount) {
        this.connectedCount = connectedCount;
        return this;
    }

    /** Get the cluster server state. */
    public ClusterServerStateVO getState() {
        return state;
    }

    /** Set the cluster server state and return this. */
    public AppClusterServerStateWrapVO setState(ClusterServerStateVO state) {
        this.state = state;
        return this;
    }

    /** Return a string representation of this VO. */
    @Override
    public String toString() {
        return "AppClusterServerStateWrapVO{" +
            "id='" + id + '\'' +
            ", ip='" + ip + '\'' +
            ", port='" + port + '\'' +
            ", belongToApp=" + belongToApp +
            ", state=" + state +
            '}';
    }
}
