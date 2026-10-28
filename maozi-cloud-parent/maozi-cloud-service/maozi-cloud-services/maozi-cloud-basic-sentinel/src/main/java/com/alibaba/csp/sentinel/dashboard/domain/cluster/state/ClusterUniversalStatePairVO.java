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
public class ClusterUniversalStatePairVO {

    /** The IP address of the machine. */
    private String ip;

    /** The command port of the machine. */
    private Integer commandPort;

    /** The universal cluster state of the machine. */
    private ClusterUniversalStateVO state;

    /** Create an empty pair. */
    public ClusterUniversalStatePairVO() {}

    /** Create a pair with the machine IP, command port and its universal cluster state. */
    public ClusterUniversalStatePairVO(String ip, Integer commandPort, ClusterUniversalStateVO state) {
        this.ip = ip;
        this.commandPort = commandPort;
        this.state = state;
    }

    /** Get the IP address. */
    public String getIp() {
        return ip;
    }

    /** Set the IP address and return this. */
    public ClusterUniversalStatePairVO setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Get the command port. */
    public Integer getCommandPort() {
        return commandPort;
    }

    /** Set the command port and return this. */
    public ClusterUniversalStatePairVO setCommandPort(Integer commandPort) {
        this.commandPort = commandPort;
        return this;
    }

    /** Get the universal cluster state. */
    public ClusterUniversalStateVO getState() {
        return state;
    }

    /** Set the universal cluster state and return this. */
    public ClusterUniversalStatePairVO setState(ClusterUniversalStateVO state) {
        this.state = state;
        return this;
    }

    /** Return a string representation of this VO. */
    @Override
    public String toString() {
        return "ClusterUniversalStatePairVO{" +
            "ip='" + ip + '\'' +
            ", commandPort=" + commandPort +
            ", state=" + state +
            '}';
    }
}
