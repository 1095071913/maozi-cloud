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

import java.util.HashSet;
import java.util.Set;

/**
 * @author Eric Zhao
 * @since 1.4.1
 */
public class ClusterGroupEntity {

    /** The machine id of the token server. */
    private String machineId;

    /** The ip of the token server. */
    private String ip;
    /** The port of the token server. */
    private Integer port;

    /** The set of machine ids of the clients bound to the token server. */
    private Set<String> clientSet = new HashSet<>();

    /** Whether the token server belongs to the current application. */
    private Boolean belongToApp;

    /** Get the machine id of the token server. */
    public String getMachineId() {
        return machineId;
    }

    /** Set the machine id of the token server and return this. */
    public ClusterGroupEntity setMachineId(String machineId) {
        this.machineId = machineId;
        return this;
    }

    /** Get the ip of the token server. */
    public String getIp() {
        return ip;
    }

    /** Set the ip of the token server and return this. */
    public ClusterGroupEntity setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Get the port of the token server. */
    public Integer getPort() {
        return port;
    }

    /** Set the port of the token server and return this. */
    public ClusterGroupEntity setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** Get the set of machine ids of the bound clients. */
    public Set<String> getClientSet() {
        return clientSet;
    }

    /** Set the set of bound clients and return this. */
    public ClusterGroupEntity setClientSet(Set<String> clientSet) {
        this.clientSet = clientSet;
        return this;
    }

    /** Whether the token server belongs to the current application. */
    public Boolean getBelongToApp() {
        return belongToApp;
    }

    /** Set whether the token server belongs to the application and return this. */
    public ClusterGroupEntity setBelongToApp(Boolean belongToApp) {
        this.belongToApp = belongToApp;
        return this;
    }

    /** Get the string representation of this entity. */
    @Override
    public String toString() {
        return "ClusterGroupEntity{" +
            "machineId='" + machineId + '\'' +
            ", ip='" + ip + '\'' +
            ", port=" + port +
            ", clientSet=" + clientSet +
            ", belongToApp=" + belongToApp +
            '}';
    }
}
