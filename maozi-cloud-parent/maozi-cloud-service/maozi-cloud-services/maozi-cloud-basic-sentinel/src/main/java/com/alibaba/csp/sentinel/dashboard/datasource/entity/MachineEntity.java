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
public class MachineEntity {
    /** The entity id. */
    private Long id;
    /** The create time. */
    private Date gmtCreate;
    /** The last modify time. */
    private Date gmtModified;
    /** The application name. */
    private String app;
    /** The IP address of the machine. */
    private String ip;
    /** The hostname of the machine. */
    private String hostname;
    /** The heartbeat timestamp of the machine. */
    private Date timestamp;
    /** The port of the machine. */
    private Integer port;

    /** Get the entity id. */
    public long getId() {
        return id;
    }

    /** Set the entity id. */
    public void setId(long id) {
        this.id = id;
    }

    /** Get the create time. */
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /** Set the create time. */
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /** Get the last modify time. */
    public Date getGmtModified() {
        return gmtModified;
    }

    /** Set the last modify time. */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    /** Get the application name. */
    public String getApp() {
        return app;
    }

    /** Set the application name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the IP address of the machine. */
    public String getIp() {
        return ip;
    }

    /** Set the IP address of the machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Get the hostname of the machine. */
    public String getHostname() {
        return hostname;
    }

    /** Set the hostname of the machine. */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** Get the heartbeat timestamp of the machine. */
    public Date getTimestamp() {
        return timestamp;
    }

    /** Set the heartbeat timestamp of the machine. */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /** Get the port of the machine. */
    public Integer getPort() {
        return port;
    }

    /** Set the port of the machine. */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Convert to a {@link MachineInfo}, using the timestamp of this entity as both
     * lastHeartbeat and heartbeatVersion.
     */
    public MachineInfo toMachineInfo() {
        MachineInfo machineInfo = new MachineInfo();

        machineInfo.setApp(app);
        machineInfo.setHostname(hostname);
        machineInfo.setIp(ip);
        machineInfo.setPort(port);
        machineInfo.setLastHeartbeat(timestamp.getTime());
        machineInfo.setHeartbeatVersion(timestamp.getTime());

        return machineInfo;
    }

    /** Build a string with all fields of this entity. */
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
