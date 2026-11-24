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

import java.util.List;
import java.util.Set;

/**
 * 已发现应用及其机器的注册表接口。
 * @author maozi
 * @date 2026/11/22
 */
public interface MachineDiscovery {

    /**
     * 应用尚未注册（集群未启动）时机器使用的应用名占位符。
     */
    String UNKNOWN_APP_NAME = "CLUSTER_NOT_STARTED";

    /**
     * 获取所有已发现应用的名称列表。
     *
     * @return 应用名称列表
     */
    List<String> getAppNames();

    /**
     * 获取所有已发现应用的简要信息。
     *
     * @return 应用简要信息集合
     */
    Set<AppInfo> getBriefApps();

    /**
     * 获取指定应用的详细信息。
     *
     * @param app 应用名
     * @return 应用信息，不存在时返回 null
     */
    AppInfo getDetailApp(String app);

    /**
     * 从应用注册表中移除指定应用。
     *
     * @param app 应用名
     * @since 1.5.0
     */
    void removeApp(String app);

    /**
     * 向应用注册表注册机器。默认内存实现固定返回 1。
     *
     * @param machineInfo 机器信息
     * @return 注册结果，默认实现固定返回 1
     */
    long addMachine(MachineInfo machineInfo);

    /**
     * 从应用注册表中移除指定机器。
     *
     * @param app  机器所属应用名
     * @param ip   机器 IP
     * @param port 机器端口
     * @return 移除成功返回 true，否则返回 false
     * @since 1.5.0
     */
    boolean removeMachine(String app, String ip, int port);
}
