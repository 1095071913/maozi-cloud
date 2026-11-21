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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用信息，包含应用名、应用类型及注册到该应用的机器集合。
 * @author maozi
 * @date 2026/11/22
 */
public class AppInfo {

    /** 应用名。 */
    private String app = "";

    /** 应用类型。 */
    private Integer appType = 0;

    /** 注册到该应用的机器集合。 */
    private Set<MachineInfo> machines = ConcurrentHashMap.newKeySet();

    /** 默认构造方法。 */
    public AppInfo() {}

    /** 创建指定应用名的应用信息。 */
    public AppInfo(String app) {
        this.app = app;
    }

    /** 创建指定应用名和应用类型的应用信息。 */
    public AppInfo(String app, Integer appType) {
        this.app = app;
        this.appType = appType;
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

    /**
     * 获取当前机器集合。
     *
     * @return 当前机器集合的副本
     */
    public Set<MachineInfo> getMachines() {
        return new HashSet<>(machines);
    }

    /** 返回包含应用名和机器集合的字符串表示。 */
    @Override
    public String toString() {
        return "AppInfo{" + "app='" + app + ", machines=" + machines + '}';
    }

    /**
     * 将机器加入本应用，先移除已存在的相同机器（按 app、ip、port 匹配）再添加。
     *
     * @param machineInfo 机器信息
     * @return 添加成功返回 true
     */
    public boolean addMachine(MachineInfo machineInfo) {
        machines.remove(machineInfo);
        return machines.add(machineInfo);
    }

    /**
     * 移除本应用中指定 ip 和 port 的机器。
     *
     * @param ip   机器 IP
     * @param port 机器端口
     * @return 找到并移除返回 true，否则返回 false
     */
    public synchronized boolean removeMachine(String ip, int port) {
        Iterator<MachineInfo> it = machines.iterator();
        while (it.hasNext()) {
            MachineInfo machine = it.next();
            if (Objects.equals(machine.getIp(), ip) && machine.getPort() == port) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定 ip 和 port 的机器。
     *
     * @param ip   机器 IP
     * @param port 机器端口
     * @return 匹配的机器，不存在时返回 Optional.empty()
     */
    public Optional<MachineInfo> getMachine(String ip, int port) {
        return machines.stream()
            .filter(e -> Objects.equals(e.getIp(), ip) && e.getPort().equals(port))
            .findFirst();
    }

    /**
     * 获取指定 ip 的第一台机器。
     *
     * @param ip 机器 IP
     * @return 匹配的机器，不存在时返回 Optional.empty()
     */
    public Optional<MachineInfo> getMachine(String ip) {
        return machines.stream()
            .filter(e -> Objects.equals(e.getIp(), ip))
            .findFirst();
    }

    /**
     * 判断应用在指定心跳超时阈值（毫秒）内是否仍有存活机器。
     * <p>无机器视为失活；阈值大于 0 时，存在任一健康机器即存活，
     * 无健康机器时仅当全部机器中最新的心跳距当前时间在阈值内才视为存活。
     */
    private boolean heartbeatJudge(final int threshold) {
        if (machines.isEmpty()) {
            return false;
        }
        if (threshold > 0) {
            long healthyCount = machines.stream()
                .filter(MachineInfo::isHealthy)
                .count();
            if (healthyCount == 0) {
                // 无健康机器时，退化为按最新心跳时间判断
                return machines.stream()
                    .max(Comparator.comparingLong(MachineInfo::getLastHeartbeat))
                    .map(e -> System.currentTimeMillis() - e.getLastHeartbeat() < threshold)
                    .orElse(false);
            }
        }
        return true;
    }

    /**
     * 判断当前应用是否在侧边栏展示。
     *
     * @return 展示返回 true，否则返回 false
     */
    public boolean isShown() {
        return heartbeatJudge(DashboardConfig.getHideAppNoMachineMillis());
    }

    /**
     * 判断当前应用是否已无健康机器、需要移除。
     *
     * @return 已失活需移除返回 true，否则返回 false
     */
    public boolean isDead() {
        return !heartbeatJudge(DashboardConfig.getRemoveAppNoMachineMillis());
    }
}
