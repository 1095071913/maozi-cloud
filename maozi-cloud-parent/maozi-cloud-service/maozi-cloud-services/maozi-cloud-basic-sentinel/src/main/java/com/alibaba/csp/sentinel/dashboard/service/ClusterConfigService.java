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
package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterGroupEntity;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerFlowConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterClientModifyRequest;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterServerModifyRequest;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterClientStateVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStatePairVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStateVO;
import com.alibaba.csp.sentinel.dashboard.util.AsyncUtils;
import com.alibaba.csp.sentinel.dashboard.util.ClusterEntityUtils;
import com.alibaba.csp.sentinel.dashboard.util.MachineUtils;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 查询应用机器集群状态并通过 Sentinel 命令 API 修改集群客户端/服务端配置的服务
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
@Service
public class ClusterConfigService {

    /** 向机器发送集群状态/配置请求的客户端 */
    private final SentinelApiClient sentinelApiClient;
    /** 提供应用及其机器信息的注册表 */
    private final AppManagement appManagement;

    /** 注入 Sentinel API 客户端与应用注册表 */
    public ClusterConfigService(SentinelApiClient sentinelApiClient, AppManagement appManagement) {
        this.sentinelApiClient = sentinelApiClient;
        this.appManagement = appManagement;
    }

    /**
     * 修改请求机器的集群客户端配置，随后将机器切换为 Token 客户端模式。
     * 请求不合法时抛出 {@link IllegalArgumentException}
     */
    public CompletableFuture<Void> modifyClusterClientConfig(ClusterClientModifyRequest request) {
        if (notClientRequestValid(request)) {
            throw new IllegalArgumentException("Invalid request");
        }
        String app = request.getApp();
        String ip = request.getIp();
        int port = request.getPort();
        return sentinelApiClient.modifyClusterClientConfig(app, ip, port, request.getClientConfig())
                .thenCompose(v -> sentinelApiClient.modifyClusterMode(ip, port, ClusterStateManager.CLUSTER_CLIENT));
    }

    /**
     * 判断客户端请求是否不合法：客户端配置为 null、serverHost 为空，
     * 或 serverPort/requestTimeout 缺失及非正数
     */
    private boolean notClientRequestValid(ClusterClientModifyRequest request) {
        ClusterClientConfig config = request.getClientConfig();
        if (config == null) {
            return true;
        }
        Integer serverPort = config.getServerPort();
        Integer requestTimeout = config.getRequestTimeout();
        return StringUtil.isEmpty(config.getServerHost())
                || serverPort == null || serverPort <= 0
                || requestTimeout == null || requestTimeout <= 0;
    }

    /**
     * 修改请求机器的集群服务端配置：依次推送命名空间集合、传输配置与流控配置，
     * 随后将机器切换为 Token 服务端模式。任一配置不合法或命名空间集合为
     * null 时抛出 {@link IllegalArgumentException}
     */
    public CompletableFuture<Void> modifyClusterServerConfig(ClusterServerModifyRequest request) {
        ServerTransportConfig transportConfig = request.getTransportConfig();
        ServerFlowConfig flowConfig = request.getFlowConfig();
        Set<String> namespaceSet = request.getNamespaceSet();
        if (invalidTransportConfig(transportConfig)) {
            throw new IllegalArgumentException("Invalid transport config in request");
        }
        if (invalidFlowConfig(flowConfig)) {
            throw new IllegalArgumentException("Invalid flow config in request");
        }
        if (namespaceSet == null) {
            throw new IllegalArgumentException("namespace set cannot be null");
        }
        String app = request.getApp();
        String ip = request.getIp();
        int port = request.getPort();
        return sentinelApiClient.modifyClusterServerNamespaceSet(app, ip, port, namespaceSet)
                .thenCompose(v -> sentinelApiClient.modifyClusterServerTransportConfig(app, ip, port, transportConfig))
                .thenCompose(v -> sentinelApiClient.modifyClusterServerFlowConfig(app, ip, port, flowConfig))
                .thenCompose(v -> sentinelApiClient.modifyClusterMode(ip, port, ClusterStateManager.CLUSTER_SERVER));
    }

    /**
     * 获取指定应用全部可用机器的集群状态列表
     *
     * @param app 应用名
     * @return 该应用全部可用机器的集群状态列表
     * @since 1.4.1
     */
    public CompletableFuture<List<ClusterUniversalStatePairVO>> getClusterUniversalState(String app) {
        if (StringUtil.isBlank(app)) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("app cannot be empty"));
        }
        AppInfo appInfo = appManagement.getDetailApp(app);
        if (appInfo == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        Set<MachineInfo> machines = appInfo.getMachines();
        if (machines == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<ClusterUniversalStatePairVO>> futures = machines.stream()
                .filter(MachineInfo::isHealthy)
                .map(machine -> {
                    String machineIp = machine.getIp();
                    int machinePort = machine.getPort();
                    return getClusterUniversalState(app, machineIp, machinePort)
                            .thenApply(e -> new ClusterUniversalStatePairVO(machineIp, machinePort, e));
                })
                .collect(Collectors.toList());

        return AsyncUtils.sequenceSuccessFuture(futures);
    }

    /**
     * 根据应用健康机器的集群状态，获取指定机器（machineId 为
     * {@code ip@commandPort} 形式）作为该应用 Token 服务端所属的集群分组。
     * 机器不存在、已失联或不是 Token 服务端时返回失败的 Future
     */
    public CompletableFuture<ClusterGroupEntity> getClusterUniversalStateForAppMachine(String app, String machineId) {
        if (StringUtil.isBlank(app)) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("app cannot be empty"));
        }
        AppInfo appInfo = appManagement.getDetailApp(app);
        if (appInfo == null) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("app does not have machines"));
        }
        Set<MachineInfo> machines = appInfo.getMachines();
        if (machines == null) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("app does not have machines"));
        }

        boolean machineOk = machines.stream()
                .filter(MachineInfo::isHealthy)
                .map(machine -> machine.getIp() + MachineUtils.MACHINE_ID_SEPARATOR + machine.getPort())
                .anyMatch(machineId::equals);
        if (!machineOk) {
            return AsyncUtils.newFailedFuture(new IllegalStateException("machine does not exist or disconnected"));
        }

        return getClusterUniversalState(app)
                .thenApply(ClusterEntityUtils::wrapToClusterGroup)
                .thenCompose(e -> e.stream()
                        .filter(e1 -> e1.getMachineId().equals(machineId))
                        .findAny()
                        .map(CompletableFuture::completedFuture)
                        .orElse(AsyncUtils.newFailedFuture(
                                new IllegalStateException("not a server: " + machineId)))
                );
    }

    /**
     * 拉取单台机器的集群模式，并在 Token 客户端可用时补充客户端配置、
     * Token 服务端可用时补充服务端状态到状态 VO
     */
    public CompletableFuture<ClusterUniversalStateVO> getClusterUniversalState(String app, String ip, int port) {
        return sentinelApiClient.fetchClusterMode(ip, port)
                .thenApply(e -> new ClusterUniversalStateVO().setStateInfo(e))
                .thenCompose(vo -> {
                    if (Boolean.TRUE.equals(vo.getStateInfo().getClientAvailable())) {
                        return sentinelApiClient.fetchClusterClientInfoAndConfig(ip, port)
                                .thenApply(cc -> vo.setClient(new ClusterClientStateVO().setClientConfig(cc)));
                    } else {
                        return CompletableFuture.completedFuture(vo);
                    }
                }).thenCompose(vo -> {
                    if (Boolean.TRUE.equals(vo.getStateInfo().getServerAvailable())) {
                        return sentinelApiClient.fetchClusterServerBasicInfo(ip, port)
                                .thenApply(vo::setServer);
                    } else {
                        return CompletableFuture.completedFuture(vo);
                    }
                });
    }

    /**
     * 判断传输配置是否为 null，或其 port/idleSeconds 缺失及非正数
     */
    private boolean invalidTransportConfig(ServerTransportConfig transportConfig) {
        if (transportConfig == null) {
            return true;
        }
        Integer port = transportConfig.getPort();
        Integer idleSeconds = transportConfig.getIdleSeconds();
        return port == null || port <= 0
                || idleSeconds == null || idleSeconds <= 0;
    }

    /**
     * 判断流控配置是否为 null、sampleCount 或 intervalMs 缺失及非正数、
     * intervalMs 不能被 sampleCount 整除，或 maxAllowedQps 缺失及为负数
     */
    private boolean invalidFlowConfig(ServerFlowConfig flowConfig) {
        if (flowConfig == null) {
            return true;
        }
        Integer sampleCount = flowConfig.getSampleCount();
        Integer intervalMs = flowConfig.getIntervalMs();
        Double maxAllowedQps = flowConfig.getMaxAllowedQps();
        return sampleCount == null || sampleCount <= 0
                || intervalMs == null || intervalMs <= 0
                || intervalMs % sampleCount != 0
                || maxAllowedQps == null || maxAllowedQps < 0;
    }
}
