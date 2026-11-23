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
 * 机器信息，即应用的一个 Sentinel 客户端实例。
 * @author maozi
 * @date 2026/11/22
 */
public class MachineInfo implements Comparable<MachineInfo> {

    /** 应用名。 */
    private String app = "";
    /** 应用类型。 */
    private Integer appType = 0;
    /** 机器主机名。 */
    private String hostname = "";
    /** 机器 IP 地址。 */
    private String ip = "";
    /** 机器命令端口。 */
    private Integer port = -1;
    /** 最近一次心跳时间戳。 */
    private long lastHeartbeat;
    /** 最近一次心跳消息携带的版本号。 */
    private long heartbeatVersion;

    /** Sentinel 客户端版本（0.2.0 起支持）。 */
    private String version;

    /** 根据应用名、IP、端口构建机器信息。 */
    public static MachineInfo of(String app, String ip, Integer port) {
        MachineInfo machineInfo = new MachineInfo();
        machineInfo.setApp(app);
        machineInfo.setIp(ip);
        machineInfo.setPort(port);
        return machineInfo;
    }

    /** 获取 "ip:port" 形式的机器地址。 */
    public String toHostPort() {
        return ip + ":" + port;
    }

    /** 获取机器命令端口。 */
    public Integer getPort() {
        return port;
    }

    /** 设置机器命令端口。 */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** 获取应用名。 */
    public String getApp() {
        return app;
    }

    /** 设置应用名。 */
    public void setApp(String app) {
        this.app = app;
    }

    /** 获取应用类型。 */
    public Integer getAppType() {
        return appType;
    }

    /** 设置应用类型。 */
    public void setAppType(Integer appType) {
        this.appType = appType;
    }

    /** 获取机器主机名。 */
    public String getHostname() {
        return hostname;
    }

    /** 设置机器主机名。 */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** 获取机器 IP 地址。 */
    public String getIp() {
        return ip;
    }

    /** 设置机器 IP 地址。 */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** 获取心跳版本号。 */
    public long getHeartbeatVersion() {
        return heartbeatVersion;
    }
    
    /** 设置心跳版本号。 */
    public void setHeartbeatVersion(long heartbeatVersion) {
        this.heartbeatVersion = heartbeatVersion;
    }

    /** 获取 Sentinel 客户端版本。 */
    public String getVersion() {
        return version;
    }

    /** 设置 Sentinel 客户端版本并返回当前对象。 */
    public MachineInfo setVersion(String version) {
        this.version = version;
        return this;
    }
    
    /** 机器是否健康（最近一次心跳距当前时间小于不健康阈值）。 */
    public boolean isHealthy() {
        long delta = System.currentTimeMillis() - lastHeartbeat;
        return delta < DashboardConfig.getUnhealthyMachineMillis();
    }

    /** 获取最近一次心跳时间戳。 */
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    /** 设置最近一次心跳时间戳。 */
    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    /** 先按端口比较，再按应用名（忽略大小写）、IP（忽略大小写）比较。 */
    @Override
    public int compareTo(MachineInfo o) {
        if (this == o) {
            return 0;
        }
        // 缓存对方端口与应用名，避免同一方法内重复调用
        Integer otherPort = o.getPort();
        if (!port.equals(otherPort)) {
            return port.compareTo(otherPort);
        }
        String otherApp = o.getApp();
        if (!StringUtil.equals(app, otherApp)) {
            return app.compareToIgnoreCase(otherApp);
        }
        return ip.compareToIgnoreCase(o.getIp());
    }

    /** 返回包含机器全部字段的字符串表示。 */
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

    /** 两台机器的 app、ip、port 均相同则视为相等。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof MachineInfo)) { return false; }
        MachineInfo that = (MachineInfo)o;
        return Objects.equals(app, that.app) &&
            Objects.equals(ip, that.ip) &&
            Objects.equals(port, that.port);
    }

    /** 由 app、ip、port 计算哈希值。 */
    @Override
    public int hashCode() {
        return Objects.hash(app, ip, port);
    }

    /**
     * 用于日志输出的机器信息。
     *
     * @return "app|ip|port|version" 格式的紧凑日志字符串
     */
    public String toLogString() {
        return app + "|" + ip + "|" + port + "|" + version;
    }
}
