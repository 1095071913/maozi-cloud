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
import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for querying the cluster state of app machines and modifying cluster
 * client/server configuration via the Sentinel command API.
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
@Service
public class ClusterConfigService {

    /** Client for sending cluster state/config requests to machines. */
    @Autowired
    private SentinelApiClient sentinelApiClient;
    /** Registry providing the applications and their machines. */
    @Autowired
    private AppManagement appManagement;

    /**
     * Modifies the cluster client config of the requesting machine and then switches
     * the machine to token-client mode. Throws {@link IllegalArgumentException} when
     * the request is invalid.
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
     * Returns whether the client request is invalid, i.e. the client config is null,
     * its serverHost is blank, or its serverPort or requestTimeout is missing or
     * non-positive.
     */
    private boolean notClientRequestValid(/*@NonNull */ ClusterClientModifyRequest request) {
        ClusterClientConfig config = request.getClientConfig();
        return config == null || StringUtil.isEmpty(config.getServerHost())
            || config.getServerPort() == null || config.getServerPort() <= 0
            || config.getRequestTimeout() == null || config.getRequestTimeout() <= 0;
    }

    /**
     * Modifies the cluster server config of the requesting machine: namespace set,
     * transport config and flow config are pushed in sequence, then the machine is
     * switched to token-server mode. Throws {@link IllegalArgumentException} when any
     * config is invalid or the namespace set is null.
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
     * Get cluster state list of all available machines of provided application.
     *
     * @param app application name
     * @return cluster state list of all available machines of the application
     * @since 1.4.1
     */
    public CompletableFuture<List<ClusterUniversalStatePairVO>> getClusterUniversalState(String app) {
        if (StringUtil.isBlank(app)) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("app cannot be empty"));
        }
        AppInfo appInfo = appManagement.getDetailApp(app);
        if (appInfo == null || appInfo.getMachines() == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<ClusterUniversalStatePairVO>> futures = appInfo.getMachines().stream()
            .filter(e -> e.isHealthy())
            .map(machine -> getClusterUniversalState(app, machine.getIp(), machine.getPort())
                .thenApply(e -> new ClusterUniversalStatePairVO(machine.getIp(), machine.getPort(), e)))
            .collect(Collectors.toList());

        return AsyncUtils.sequenceSuccessFuture(futures);
    }

    /**
     * Gets the cluster group that the given machine (machineId in {@code ip@commandPort}
     * form) belongs to as a token server of the app, derived from the cluster state of
     * the app's healthy machines. The future fails when the machine does not exist, is
     * disconnected, or is not a token server.
     */
    public CompletableFuture<ClusterGroupEntity> getClusterUniversalStateForAppMachine(String app, String machineId) {
        if (StringUtil.isBlank(app)) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("app cannot be empty"));
        }
        AppInfo appInfo = appManagement.getDetailApp(app);
        if (appInfo == null || appInfo.getMachines() == null) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("app does not have machines"));
        }

        boolean machineOk = appInfo.getMachines().stream()
            .filter(e -> e.isHealthy())
            .map(e -> e.getIp() + '@' + e.getPort())
            .anyMatch(e -> e.equals(machineId));
        if (!machineOk) {
            return AsyncUtils.newFailedFuture(new IllegalStateException("machine does not exist or disconnected"));
        }

        return getClusterUniversalState(app)
            .thenApply(ClusterEntityUtils::wrapToClusterGroup)
            .thenCompose(e -> e.stream()
                .filter(e1 -> e1.getMachineId().equals(machineId))
                .findAny()
                .map(CompletableFuture::completedFuture)
                .orElse(AsyncUtils.newFailedFuture(new IllegalStateException("not a server: " + machineId)))
            );
    }

    /**
     * Fetches the cluster mode of a single machine and enriches the state VO with the
     * client config (when the token client is available) and the server state (when
     * the token server is available).
     */
    public CompletableFuture<ClusterUniversalStateVO> getClusterUniversalState(String app, String ip, int port) {
        return sentinelApiClient.fetchClusterMode(ip, port)
            .thenApply(e -> new ClusterUniversalStateVO().setStateInfo(e))
            .thenCompose(vo -> {
                if (vo.getStateInfo().getClientAvailable()) {
                    return sentinelApiClient.fetchClusterClientInfoAndConfig(ip, port)
                        .thenApply(cc -> vo.setClient(new ClusterClientStateVO().setClientConfig(cc)));
                } else {
                    return CompletableFuture.completedFuture(vo);
                }
            }).thenCompose(vo -> {
                if (vo.getStateInfo().getServerAvailable()) {
                    return sentinelApiClient.fetchClusterServerBasicInfo(ip, port)
                        .thenApply(vo::setServer);
                } else {
                    return CompletableFuture.completedFuture(vo);
                }
            });
    }

    /**
     * Returns whether the transport config is null or its port/idleSeconds is missing
     * or non-positive.
     */
    private boolean invalidTransportConfig(ServerTransportConfig transportConfig) {
        return transportConfig == null || transportConfig.getPort() == null || transportConfig.getPort() <= 0
            || transportConfig.getIdleSeconds() == null || transportConfig.getIdleSeconds() <= 0;
    }

    /**
     * Returns whether the flow config is null, its sampleCount or intervalMs is missing
     * or non-positive, its intervalMs is not divisible by sampleCount, or its
     * maxAllowedQps is missing or negative.
     */
    private boolean invalidFlowConfig(ServerFlowConfig flowConfig) {
        return flowConfig == null || flowConfig.getSampleCount() == null || flowConfig.getSampleCount() <= 0
            || flowConfig.getIntervalMs() == null || flowConfig.getIntervalMs() <= 0
            || flowConfig.getIntervalMs() % flowConfig.getSampleCount() != 0
            || flowConfig.getMaxAllowedQps() == null || flowConfig.getMaxAllowedQps() < 0;
    }
}
