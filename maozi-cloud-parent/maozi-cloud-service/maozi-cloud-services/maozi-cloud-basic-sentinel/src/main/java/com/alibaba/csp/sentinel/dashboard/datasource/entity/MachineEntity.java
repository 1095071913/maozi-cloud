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
 * 注册到控制台的机器（应用实例）持久化实体。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class MachineEntity extends BaseEntity {
    /** 机器 IP 地址。 */
    private String ip;
    /** 机器主机名。 */
    private String hostname;
    /** 机器心跳时间戳。 */
    private Date timestamp;
    /** 机器端口。 */
    private Integer port;

    /** 获取机器 IP 地址。 */
    public String getIp() {
        return ip;
    }

    /** 设置机器 IP 地址。 */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** 获取机器主机名。 */
    public String getHostname() {
        return hostname;
    }

    /** 设置机器主机名。 */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** 获取机器心跳时间戳。 */
    public Date getTimestamp() {
        return timestamp;
    }

    /** 设置机器心跳时间戳。 */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /** 获取机器端口。 */
    public Integer getPort() {
        return port;
    }

    /** 设置机器端口。 */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * 转换为 {@link MachineInfo}，本实体的时间戳同时作为
     * lastHeartbeat 与 heartbeatVersion。
     */
    public MachineInfo toMachineInfo() {
        MachineInfo machineInfo = new MachineInfo();

        machineInfo.setApp(app);
        machineInfo.setHostname(hostname);
        machineInfo.setIp(ip);
        machineInfo.setPort(port);
        Long heartbeatTimestamp = timestamp.getTime();
        machineInfo.setLastHeartbeat(heartbeatTimestamp);
        machineInfo.setHeartbeatVersion(heartbeatTimestamp);

        return machineInfo;
    }

    /** 拼接全部字段构建字符串。 */
    @Override
    public String toString() {
        return "MachineEntity{"
                + "id=" + id
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + ", app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", hostname='" + hostname + '\''
                + ", timestamp=" + timestamp
                + ", port=" + port
                + '}';
    }
}
