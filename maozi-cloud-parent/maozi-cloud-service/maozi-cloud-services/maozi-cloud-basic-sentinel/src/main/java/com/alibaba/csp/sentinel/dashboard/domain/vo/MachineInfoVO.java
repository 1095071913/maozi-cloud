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
package com.alibaba.csp.sentinel.dashboard.domain.vo;

import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 机器管理页面展示的机器信息视图对象。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class MachineInfoVO {

    /** 机器所属应用名。 */
    private String app;

    /** 机器主机名。 */
    private String hostname;

    /** 机器 IP 地址。 */
    private String ip;

    /** 机器命令端口。 */
    private Integer port;

    /** 心跳版本号。 */
    private Long heartbeatVersion;

    /** 最近一次心跳时间戳。 */
    private Long lastHeartbeat;

    /** 机器是否健康。 */
    private Boolean healthy;

    /** 机器的 Sentinel 客户端版本。 */
    private String version;

    /** 将机器信息列表转换为对应的 VO 列表。 */
    public static List<MachineInfoVO> fromMachineInfoList(List<MachineInfo> machines) {
        List<MachineInfoVO> list = new ArrayList<>(machines.size());
        for (MachineInfo machine : machines) {
            list.add(fromMachineInfo(machine));
        }
        return list;
    }

    /** 将指定机器信息的各字段复制到新的 VO。 */
    public static MachineInfoVO fromMachineInfo(MachineInfo machine) {
        MachineInfoVO vo = new MachineInfoVO();
        vo.setApp(machine.getApp());
        vo.setHostname(machine.getHostname());
        vo.setIp(machine.getIp());
        vo.setPort(machine.getPort());
        vo.setLastHeartbeat(machine.getLastHeartbeat());
        vo.setHeartbeatVersion(machine.getHeartbeatVersion());
        vo.setVersion(machine.getVersion());
        vo.setHealthy(machine.isHealthy());
        return vo;
    }

    /** 获取应用名。 */
    public String getApp() {
        return app;
    }

    /** 设置应用名。 */
    public void setApp(String app) {
        this.app = app;
    }

    /** 获取机器主机名。 */
    public String getHostname() {
        return hostname;
    }

    /** 设置机器主机名。 */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** 获取 IP 地址。 */
    public String getIp() {
        return ip;
    }

    /** 设置 IP 地址。 */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** 获取命令端口。 */
    public Integer getPort() {
        return port;
    }

    /** 设置命令端口。 */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** 获取最近一次心跳时间戳。 */
    public Long getLastHeartbeat() {
        return lastHeartbeat;
    }

    /** 设置最近一次心跳时间戳。 */
    public void setLastHeartbeat(Long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    /** 设置心跳版本号。 */
    public void setHeartbeatVersion(Long heartbeatVersion) {
        this.heartbeatVersion = heartbeatVersion;
    }

    /** 获取心跳版本号。 */
    public Long getHeartbeatVersion() {
        return heartbeatVersion;
    }

    /** 获取 Sentinel 客户端版本。 */
    public String getVersion() {
        return version;
    }

    /** 设置 Sentinel 客户端版本并返回当前对象。 */
    public MachineInfoVO setVersion(String version) {
        this.version = version;
        return this;
    }

    /** 机器是否健康。 */
    public Boolean getHealthy() {
        return healthy;
    }

    /** 设置机器是否健康。 */
    public void setHealthy(Boolean healthy) {
        this.healthy = healthy;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "MachineInfoVO{"
                + "app='" + app + '\''
                + ", hostname='" + hostname + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", heartbeatVersion=" + heartbeatVersion
                + ", lastHeartbeat=" + lastHeartbeat
                + ", healthy=" + healthy
                + ", version='" + version + '\''
                + '}';
    }
}
