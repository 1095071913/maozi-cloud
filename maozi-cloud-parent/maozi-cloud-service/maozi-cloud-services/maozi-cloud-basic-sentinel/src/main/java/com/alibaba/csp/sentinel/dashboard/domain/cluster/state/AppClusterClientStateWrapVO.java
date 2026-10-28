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
public class AppClusterClientStateWrapVO {

    /**
     * {ip}@{transport_command_port}.
     */
    private String id;

    /** The command port of the machine. */
    private Integer commandPort;

    /** The IP address of the machine. */
    private String ip;

    /** The cluster client state of the machine. */
    private ClusterClientStateVO state;

    /** Get the machine ID. */
    public String getId() {
        return id;
    }

    /** Set the machine ID and return this. */
    public AppClusterClientStateWrapVO setId(String id) {
        this.id = id;
        return this;
    }

    /** Get the IP address. */
    public String getIp() {
        return ip;
    }

    /** Set the IP address and return this. */
    public AppClusterClientStateWrapVO setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Get the cluster client state. */
    public ClusterClientStateVO getState() {
        return state;
    }

    /** Set the cluster client state and return this. */
    public AppClusterClientStateWrapVO setState(ClusterClientStateVO state) {
        this.state = state;
        return this;
    }

    /** Get the command port. */
    public Integer getCommandPort() {
        return commandPort;
    }

    /** Set the command port and return this. */
    public AppClusterClientStateWrapVO setCommandPort(Integer commandPort) {
        this.commandPort = commandPort;
        return this;
    }

    /** Return a string representation of this VO. */
    @Override
    public String toString() {
        return "AppClusterClientStateWrapVO{" +
            "id='" + id + '\'' +
            ", commandPort=" + commandPort +
            ", ip='" + ip + '\'' +
            ", state=" + state +
            '}';
    }
}
