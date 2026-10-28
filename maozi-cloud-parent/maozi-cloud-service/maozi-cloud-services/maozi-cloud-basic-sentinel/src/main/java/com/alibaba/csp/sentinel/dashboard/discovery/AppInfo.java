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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information of an application, including its name, type and the machines registered to it.
 */
public class AppInfo {

    /** The name of the application. */
    private String app = "";

    /** The type of the application. */
    private Integer appType = 0;

    /** The machines registered to this application. */
    private Set<MachineInfo> machines = ConcurrentHashMap.newKeySet();

    /** Default constructor. */
    public AppInfo() {}

    /** Create an AppInfo with the given app name. */
    public AppInfo(String app) {
        this.app = app;
    }

    /** Create an AppInfo with the given app name and app type. */
    public AppInfo(String app, Integer appType) {
        this.app = app;
        this.appType = appType;
    }

    /** Get the name of the application. */
    public String getApp() {
        return app;
    }

    /** Set the name of the application. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the type of the application. */
    public Integer getAppType() {
        return appType;
    }

    /** Set the type of the application. */
    public void setAppType(Integer appType) {
        this.appType = appType;
    }

    /**
     * Get the current machines.
     *
     * @return a new copy of the current machines.
     */
    public Set<MachineInfo> getMachines() {
        return new HashSet<>(machines);
    }

    /** Contains the app name and the machine set. */
    @Override
    public String toString() {
        return "AppInfo{" + "app='" + app + ", machines=" + machines + '}';
    }

    /**
     * Add the machine to this application, removing the existing equal machine
     * (matched by app, ip and port) first.
     */
    public boolean addMachine(MachineInfo machineInfo) {
        machines.remove(machineInfo);
        return machines.add(machineInfo);
    }

    /**
     * Remove the machine with the given ip and port from this application,
     * returning true only if a matching machine was found and removed.
     */
    public synchronized boolean removeMachine(String ip, int port) {
        Iterator<MachineInfo> it = machines.iterator();
        while (it.hasNext()) {
            MachineInfo machine = it.next();
            if (machine.getIp().equals(ip) && machine.getPort() == port) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /** Get the machine with the given ip and port, if present. */
    public Optional<MachineInfo> getMachine(String ip, int port) {
        return machines.stream()
            .filter(e -> e.getIp().equals(ip) && e.getPort().equals(port))
            .findFirst();
    }

    /** Get the first machine matching the given ip, if present. */
    public Optional<MachineInfo> getMachine(String ip) {
        return machines.stream()
            .filter(e -> e.getIp().equals(ip))
            .findFirst();
    }

    /**
     * Check whether the application still has a heartbeat within the given threshold (ms).
     * An app with no machine is dead; when the threshold is positive, an app with any
     * healthy machine is alive, while an app with no healthy machine is alive only if
     * the latest heartbeat among all machines is within the threshold.
     */
    private boolean heartbeatJudge(final int threshold) {
        if (machines.size() == 0) {
            return false;
        }
        if (threshold > 0) {
            long healthyCount = machines.stream()
                .filter(MachineInfo::isHealthy)
                .count();
            if (healthyCount == 0) {
                // No healthy machines.
                return machines.stream()
                    .max(Comparator.comparingLong(MachineInfo::getLastHeartbeat))
                    .map(e -> System.currentTimeMillis() - e.getLastHeartbeat() < threshold)
                    .orElse(false);
            }
        }
        return true;
    }

    /**
     * Check whether current application should be displayed in the sidebar.
     *
     * @return true if the application should be displayed in the sidebar, otherwise false
     */
    public boolean isShown() {
        return heartbeatJudge(DashboardConfig.getHideAppNoMachineMillis());
    }

    /**
     * Check whether current application has no healthy machines and should be removed.
     *
     * @return true if the application is dead and should be removed, otherwise false
     */
    public boolean isDead() {
        return !heartbeatJudge(DashboardConfig.getRemoveAppNoMachineMillis());
    }
}
