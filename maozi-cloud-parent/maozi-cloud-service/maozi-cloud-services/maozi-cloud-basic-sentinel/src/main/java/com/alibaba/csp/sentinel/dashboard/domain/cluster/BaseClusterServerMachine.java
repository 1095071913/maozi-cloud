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
 * Base entity of a cluster token server machine, holding the server machine
 * identity (machineId, ip, port), the bound client set and the belongToApp flag.
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public abstract class BaseClusterServerMachine {

    /** The machine id of the token server. */
    protected String machineId;

    /** The ip of the token server. */
    protected String ip;

    /** The port of the token server. */
    protected Integer port;

    /** The set of machine ids of the clients bound to the token server. */
    protected Set<String> clientSet = new HashSet<>();

    /** Whether the token server belongs to the current application. */
    protected Boolean belongToApp;

    /** Gets the machine id of the token server. */
    public String getMachineId() {
        return machineId;
    }

    /** Sets the machine id of the token server and return this. */
    public BaseClusterServerMachine setMachineId(String machineId) {
        this.machineId = machineId;
        return this;
    }

    /** Gets the ip of the token server. */
    public String getIp() {
        return ip;
    }

    /** Sets the ip of the token server and return this. */
    public BaseClusterServerMachine setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Gets the port of the token server. */
    public Integer getPort() {
        return port;
    }

    /** Sets the port of the token server and return this. */
    public BaseClusterServerMachine setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** Gets the set of machine ids of the bound clients. */
    public Set<String> getClientSet() {
        return clientSet;
    }

    /** Sets the set of bound clients and return this. */
    public BaseClusterServerMachine setClientSet(Set<String> clientSet) {
        this.clientSet = clientSet;
        return this;
    }

    /** Whether the token server belongs to the current application. */
    public Boolean getBelongToApp() {
        return belongToApp;
    }

    /** Sets whether the token server belongs to the application and returns this. */
    public BaseClusterServerMachine setBelongToApp(Boolean belongToApp) {
        this.belongToApp = belongToApp;
        return this;
    }
}
