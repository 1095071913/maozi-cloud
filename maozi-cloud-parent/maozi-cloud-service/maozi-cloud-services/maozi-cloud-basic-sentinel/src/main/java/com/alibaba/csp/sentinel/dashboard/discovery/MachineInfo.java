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
package com.alibaba.csp.sentinel.dashboard.discovery;

import com.alibaba.csp.sentinel.dashboard.config.DashboardConfig;
import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.Objects;

/**
 * Information of a machine, i.e. a Sentinel client instance of an application.
 */
public class MachineInfo implements Comparable<MachineInfo> {

    /** The application name. */
    private String app = "";
    /** The type of the application. */
    private Integer appType = 0;
    /** The hostname of the machine. */
    private String hostname = "";
    /** The IP address of the machine. */
    private String ip = "";
    /** The command port of the machine. */
    private Integer port = -1;
    /** The timestamp of the last heartbeat. */
    private long lastHeartbeat;
    /** The version carried by the last heartbeat message. */
    private long heartbeatVersion;

    /**
     * Indicates the version of Sentinel client (since 0.2.0).
     */
    private String version;

    /** Create a machine info with the given app name, ip and port. */
    public static MachineInfo of(String app, String ip, Integer port) {
        MachineInfo machineInfo = new MachineInfo();
        machineInfo.setApp(app);
        machineInfo.setIp(ip);
        machineInfo.setPort(port);
        return machineInfo;
    }

    /** Get the machine address in "ip:port" form. */
    public String toHostPort() {
        return ip + ":" + port;
    }

    /** Get the command port of the machine. */
    public Integer getPort() {
        return port;
    }

    /** Set the command port of the machine. */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** Get the app name. */
    public String getApp() {
        return app;
    }

    /** Set the app name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the app type. */
    public Integer getAppType() {
        return appType;
    }

    /** Set the app type. */
    public void setAppType(Integer appType) {
        this.appType = appType;
    }

    /** Get the hostname. */
    public String getHostname() {
        return hostname;
    }

    /** Set the hostname. */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** Get the ip address. */
    public String getIp() {
        return ip;
    }

    /** Set the ip address. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Get the heartbeat version. */
    public long getHeartbeatVersion() {
        return heartbeatVersion;
    }
    
    /** Set the heartbeat version. */
    public void setHeartbeatVersion(long heartbeatVersion) {
        this.heartbeatVersion = heartbeatVersion;
    }

    /** Get the version of the Sentinel client. */
    public String getVersion() {
        return version;
    }

    /** Set the version of the Sentinel client and return this. */
    public MachineInfo setVersion(String version) {
        this.version = version;
        return this;
    }
    
    /** Whether the machine is healthy (the last heartbeat is within the unhealthy threshold). */
    public boolean isHealthy() {
        long delta = System.currentTimeMillis() - lastHeartbeat;
        return delta < DashboardConfig.getUnhealthyMachineMillis();
    }

    /** Get the timestamp of the last heartbeat. */
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    /** Set the timestamp of the last heartbeat. */
    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    /** Compare by port, then app name (case-insensitive), then ip (case-insensitive). */
    @Override
    public int compareTo(MachineInfo o) {
        if (this == o) {
            return 0;
        }
        if (!port.equals(o.getPort())) {
            return port.compareTo(o.getPort());
        }
        if (!StringUtil.equals(app, o.getApp())) {
            return app.compareToIgnoreCase(o.getApp());
        }
        return ip.compareToIgnoreCase(o.getIp());
    }

    /** Get the string representation containing all fields of the machine. */
    @Override
    public String toString() {
        return new StringBuilder("MachineInfo {")
            .append("app='").append(app).append('\'')
            .append(",appType='").append(appType).append('\'')
            .append(", hostname='").append(hostname).append('\'')
            .append(", ip='").append(ip).append('\'')
            .append(", port=").append(port)
            .append(", heartbeatVersion=").append(heartbeatVersion)
            .append(", lastHeartbeat=").append(lastHeartbeat)
            .append(", version='").append(version).append('\'')
            .append(", healthy=").append(isHealthy())
            .append('}').toString();
    }

    /** Whether the two machines have the same app, ip and port. */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof MachineInfo)) { return false; }
        MachineInfo that = (MachineInfo)o;
        return Objects.equals(app, that.app) &&
            Objects.equals(ip, that.ip) &&
            Objects.equals(port, that.port);
    }

    /** Get the hash code computed from app, ip and port. */
    @Override
    public int hashCode() {
        return Objects.hash(app, ip, port);
    }

    /**
     * Information for log.
     *
     * @return the compact log string of the machine ("app|ip|port|version")
     */
    public String toLogString() {
        return app + "|" + ip + "|" + port + "|" + version;
    }
}
