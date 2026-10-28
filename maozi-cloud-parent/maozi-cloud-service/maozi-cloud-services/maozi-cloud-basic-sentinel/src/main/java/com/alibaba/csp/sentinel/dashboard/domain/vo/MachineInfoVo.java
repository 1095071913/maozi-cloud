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
 * @author leyou
 */
public class MachineInfoVo {

    /** The app name of the machine. */
    private String app;

    /** The hostname of the machine. */
    private String hostname;

    /** The IP address of the machine. */
    private String ip;

    /** The command port of the machine. */
    private int port;

    /** The version of the heartbeat. */
    private long heartbeatVersion;

    /** The timestamp of the last heartbeat. */
    private long lastHeartbeat;

    /** Whether the machine is healthy. */
    private boolean healthy;

    /** The version of the Sentinel client of the machine. */
    private String version;

    /** Convert the given machine info list to the corresponding list of VOs. */
    public static List<MachineInfoVo> fromMachineInfoList(List<MachineInfo> machines) {
        List<MachineInfoVo> list = new ArrayList<>();
        for (MachineInfo machine : machines) {
            list.add(fromMachineInfo(machine));
        }
        return list;
    }

    /** Copy the fields of the given machine info into a new VO. */
    public static MachineInfoVo fromMachineInfo(MachineInfo machine) {
        MachineInfoVo vo = new MachineInfoVo();
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

    /** Get the app name. */
    public String getApp() {
        return app;
    }

    /** Set the app name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the hostname. */
    public String getHostname() {
        return hostname;
    }

    /** Set the hostname. */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** Get the IP address. */
    public String getIp() {
        return ip;
    }

    /** Set the IP address. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Get the command port. */
    public int getPort() {
        return port;
    }

    /** Set the command port. */
    public void setPort(int port) {
        this.port = port;
    }

    /** Get the timestamp of the last heartbeat. */
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    /** Set the timestamp of the last heartbeat. */
    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
    
    /** Set the version of the heartbeat. */
    public void setHeartbeatVersion(long heartbeatVersion) {
        this.heartbeatVersion = heartbeatVersion;
    }
    
    /** Get the version of the heartbeat. */
    public long getHeartbeatVersion() {
        return heartbeatVersion;
    }

    /** Get the version of the Sentinel client. */
    public String getVersion() {
        return version;
    }

    /** Set the version of the Sentinel client and return this. */
    public MachineInfoVo setVersion(String version) {
        this.version = version;
        return this;
    }

    /** Whether the machine is healthy. */
    public boolean isHealthy() {
        return healthy;
    }

    /** Set whether the machine is healthy. */
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
}
