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

import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 控制台的应用管理，将机器发现操作委托给底层的 {@link MachineDiscovery} 实现。
 * @author maozi
 * @date 2026/11/22
 */
@Component
public class AppManagement implements MachineDiscovery {

    /** Spring 应用上下文，用于获取底层机器发现实现。 */
    private final ApplicationContext context;

    /** 底层机器发现实现。 */
    private MachineDiscovery machineDiscovery;

    /** 注入 Spring 应用上下文，用于解析机器发现实现。 */
    public AppManagement(ApplicationContext context) {
        this.context = context;
    }

    /** 初始化时从容器获取 {@link SimpleMachineDiscovery} 作为底层机器发现实现。 */
    @PostConstruct
    public void init() {
        machineDiscovery = context.getBean(SimpleMachineDiscovery.class);
    }

    /** 获取所有应用的简要信息。 */
    @Override
    public Set<AppInfo> getBriefApps() {
        return machineDiscovery.getBriefApps();
    }

    /** 将机器注册到其所属应用。 */
    @Override
    public long addMachine(MachineInfo machineInfo) {
        return machineDiscovery.addMachine(machineInfo);
    }

    /** 从应用中移除指定 ip 和 port 的机器。 */
    @Override
    public boolean removeMachine(String app, String ip, int port) {
        return machineDiscovery.removeMachine(app, ip, port);
    }

    /** 获取所有应用的名称列表。 */
    @Override
    public List<String> getAppNames() {
        return machineDiscovery.getAppNames();
    }

    /** 获取指定名称的应用详细信息。 */
    @Override
    public AppInfo getDetailApp(String app) {
        return machineDiscovery.getDetailApp(app);
    }

    /** 移除指定名称的应用。 */
    @Override
    public void removeApp(String app) {
        machineDiscovery.removeApp(app);
    }

    /** 判断指定 ip 的机器是否属于该应用。 */
    public boolean isValidMachineOfApp(String app, String ip) {
        if (StringUtil.isEmpty(app)) {
            return false;
        }
        return Optional.ofNullable(getDetailApp(app))
                .flatMap(appInfo -> appInfo.getMachine(ip))
                .isPresent();
    }
}
