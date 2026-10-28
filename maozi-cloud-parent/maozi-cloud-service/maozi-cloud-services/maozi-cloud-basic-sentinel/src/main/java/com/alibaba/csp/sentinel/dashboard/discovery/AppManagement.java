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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Application management of the dashboard, which delegates machine discovery
 * operations to the underlying {@link MachineDiscovery} implementation.
 */
@Component
public class AppManagement implements MachineDiscovery {

    /** Spring application context used to resolve the underlying discovery bean. */
    @Autowired
    private ApplicationContext context;

    /** The underlying machine discovery implementation. */
    private MachineDiscovery machineDiscovery;

    /** Resolve the underlying {@link SimpleMachineDiscovery} bean as the discovery implementation. */
    @PostConstruct
    public void init() {
        machineDiscovery = context.getBean(SimpleMachineDiscovery.class);
    }

    /** Get brief info of all applications. */
    @Override
    public Set<AppInfo> getBriefApps() {
        return machineDiscovery.getBriefApps();
    }

    /** Add the machine to its application. */
    @Override
    public long addMachine(MachineInfo machineInfo) {
        return machineDiscovery.addMachine(machineInfo);
    }
    
    /** Remove the machine with the given ip and port from the application. */
    @Override
    public boolean removeMachine(String app, String ip, int port) {
        return machineDiscovery.removeMachine(app, ip, port);
    }

    /** Get the names of all applications. */
    @Override
    public List<String> getAppNames() {
        return machineDiscovery.getAppNames();
    }

    /** Get detail info of the application with the given name. */
    @Override
    public AppInfo getDetailApp(String app) {
        return machineDiscovery.getDetailApp(app);
    }
    
    /** Remove the application with the given name. */
    @Override
    public void removeApp(String app) {
        machineDiscovery.removeApp(app);
    }

    /** Check whether the machine with the given ip belongs to the application. */
    public boolean isValidMachineOfApp(String app, String ip) {
        if (StringUtil.isEmpty(app)) {
            return false;
        }
        return Optional.ofNullable(getDetailApp(app))
            .flatMap(a -> a.getMachine(ip))
            .isPresent();
    }

}
