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
package com.alibaba.csp.sentinel.dashboard.datasource.entity;

import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;

import java.util.Date;

/**
 * Persistent entity of a machine (application instance) registered to the dashboard.
 *
 * @author leyou
 */
public class MachineEntity extends BaseEntity {
    /** The IP address of the machine. */
    private String ip;
    /** The hostname of the machine. */
    private String hostname;
    /** The heartbeat timestamp of the machine. */
    private Date timestamp;
    /** The port of the machine. */
    private Integer port;

    /** Gets the IP address of the machine. */
    public String getIp() {
        return ip;
    }

    /** Sets the IP address of the machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Gets the hostname of the machine. */
    public String getHostname() {
        return hostname;
    }

    /** Sets the hostname of the machine. */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** Gets the heartbeat timestamp of the machine. */
    public Date getTimestamp() {
        return timestamp;
    }

    /** Sets the heartbeat timestamp of the machine. */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /** Gets the port of the machine. */
    public Integer getPort() {
        return port;
    }

    /** Sets the port of the machine. */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Converts to a {@link MachineInfo}, using the timestamp of this entity as both
     * lastHeartbeat and heartbeatVersion.
     */
    public MachineInfo toMachineInfo() {
        MachineInfo machineInfo = new MachineInfo();

        machineInfo.setApp(app);
        machineInfo.setHostname(hostname);
        machineInfo.setIp(ip);
        machineInfo.setPort(port);
        // Cache the heartbeat timestamp to avoid repeated calls within the same method.
        Long heartbeatTimestamp = timestamp.getTime();
        machineInfo.setLastHeartbeat(heartbeatTimestamp);
        machineInfo.setHeartbeatVersion(heartbeatTimestamp);

        return machineInfo;
    }

    /** Builds a string with all fields of this entity. */
    @Override
    public String toString() {
        return "MachineEntity{" +
            "id=" + id +
            ", gmtCreate=" + gmtCreate +
            ", gmtModified=" + gmtModified +
            ", app='" + app + '\'' +
            ", ip='" + ip + '\'' +
            ", hostname='" + hostname + '\'' +
            ", timestamp=" + timestamp +
            ", port=" + port +
            '}';
    }
}
