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

import com.alibaba.csp.sentinel.util.AssertUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link MachineDiscovery} 的默认内存实现，使用并发 Map 保存已发现的应用及其机器。
 *
 * @author leyou
 * @date 2026/11/22
 */
@Component
public class SimpleMachineDiscovery implements MachineDiscovery {

    /** 已发现的应用，key 为应用名。 */
    private final ConcurrentMap<String, AppInfo> apps = new ConcurrentHashMap<>();

    /**
     * 将机器加入其所属应用，应用不存在时先创建，固定返回 1。
     */
    @Override
    public long addMachine(MachineInfo machineInfo) {
        AssertUtil.notNull(machineInfo, "machineInfo cannot be null");
        // 缓存应用名，避免同一方法内重复获取
        // 缓存应用类型，创建应用信息时与注册机器时取值一致，避免重复获取
        String appName = machineInfo.getApp();
        Integer appType = machineInfo.getAppType();
        AppInfo appInfo = apps.computeIfAbsent(appName, key -> new AppInfo(appName, appType));
        appInfo.addMachine(machineInfo);
        return 1;
    }

    /**
     * 从应用中移除指定 ip 和 port 的机器，应用或机器不存在时返回 false。
     */
    @Override
    public boolean removeMachine(String app, String ip, int port) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        AppInfo appInfo = apps.get(app);
        if (appInfo != null) {
            return appInfo.removeMachine(ip, port);
        }
        return false;
    }

    /** 获取所有已发现应用的名称列表。 */
    @Override
    public List<String> getAppNames() {
        return new ArrayList<>(apps.keySet());
    }

    /** 获取指定名称的应用信息，应用不存在时返回 null。 */
    @Override
    public AppInfo getDetailApp(String app) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        return apps.get(app);
    }

    /** 获取所有已发现应用的简要信息。 */
    @Override
    public Set<AppInfo> getBriefApps() {
        return new HashSet<>(apps.values());
    }

    /** 移除指定名称的应用。 */
    @Override
    public void removeApp(String app) {
        AssertUtil.assertNotBlank(app, "app name cannot be blank");
        apps.remove(app);
    }
}
