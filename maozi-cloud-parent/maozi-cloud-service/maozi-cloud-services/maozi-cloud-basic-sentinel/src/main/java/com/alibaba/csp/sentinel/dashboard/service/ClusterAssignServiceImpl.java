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
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterAppAssignResultVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterGroupEntity;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerFlowConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterAppAssignMap;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStatePairVO;
import com.alibaba.csp.sentinel.dashboard.util.MachineUtils;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.function.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ClusterAssignService} that assigns token servers
 * and token clients for an application and unbinds cluster machines via the
 * Sentinel command API.
 *
 * @author Eric Zhao
 * @since 1.4.1
 */
@Service
public class ClusterAssignServiceImpl implements ClusterAssignService {

    /** Logger of this class. */
    private final Logger LOGGER = LoggerFactory.getLogger(ClusterAssignServiceImpl.class);

    /** Client for sending cluster mode/config modifications to machines. */
    @Autowired
    private SentinelApiClient sentinelApiClient;
    /** Service for querying the cluster state of app machines. */
    @Autowired
    private ClusterConfigService clusterConfigService;

    /**
     * Whether the machineId is an {@code ip:port} address, which refers to a token
     * server not registered as a machine of this app (in-app machine IDs use the
     * {@code ip@commandPort} form). Note that despite the method name, {@code true}
     * here actually means "not in app".
     */
    private boolean isMachineInApp(/*@NonEmpty*/ String machineId) {
        return machineId.contains(":");
    }

    /**
     * Unbinds a token server that is not a machine of this app (the dashboard cannot
     * reach it directly): finds all token clients of the app whose client config points
     * at the given {@code ip:port} server and resets them to NOT-STARTED mode. Failures
     * are collected in the failed client set of the result (the server address itself
     * when the state query fails).
     */
    private ClusterAppAssignResultVO handleUnbindClusterServerNotInApp(String app, String machineId) {
        Set<String> failedSet = new HashSet<>();
        try {
            List<ClusterUniversalStatePairVO> list = clusterConfigService.getClusterUniversalState(app)
                .get(10, TimeUnit.SECONDS);
            Set<String> toModifySet = list.stream()
                .filter(e -> e.getState().getStateInfo().getMode() == ClusterStateManager.CLUSTER_CLIENT)
                .filter(e -> machineId.equals(e.getState().getClient().getClientConfig().getServerHost() + ':' +
                    e.getState().getClient().getClientConfig().getServerPort()))
                .map(e -> e.getIp() + '@' + e.getCommandPort())
                .collect(Collectors.toSet());
            // Modify mode to NOT-STARTED for all associated token clients.
            modifyToNonStarted(toModifySet, failedSet);
        } catch (Exception ex) {
            Throwable e = ex instanceof ExecutionException ? ex.getCause() : ex;
            LOGGER.error("Failed to unbind machine <{}>", machineId, e);
            failedSet.add(machineId);
        }
        return new ClusterAppAssignResultVO()
            .setFailedClientSet(failedSet)
            .setFailedServerSet(new HashSet<>());
    }

    /**
     * Resets every machine in {@code toModifySet} (IDs in {@code ip@commandPort} form)
     * to NOT-STARTED cluster mode in parallel, recording failures in {@code failedSet}.
     */
    private void modifyToNonStarted(Set<String> toModifySet, Set<String> failedSet) {
        toModifySet.parallelStream()
            .map(MachineUtils::parseCommandIpAndPort)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(e -> {
                CompletableFuture<Void> f = modifyMode(e.r1, e.r2, ClusterStateManager.CLUSTER_NOT_STARTED);
                return Tuple2.of(e.r1 + '@' + e.r2, f);
            })
            .forEach(f -> handleFutureSync(f, failedSet));
    }

    /**
     * Unbinds the given machine from the cluster by resetting it and all token clients
     * assigned to it to NOT-STARTED mode. When the machineId is an {@code ip:port}
     * address of a foreign token server, only its in-app token clients are reset.
     * Failed machine IDs are collected in the failed client set of the result.
     */
    @Override
    public ClusterAppAssignResultVO unbindClusterServer(String app, String machineId) {
        AssertUtil.assertNotBlank(app, "app cannot be blank");
        AssertUtil.assertNotBlank(machineId, "machineId cannot be blank");

        if (isMachineInApp(machineId)) {
            return handleUnbindClusterServerNotInApp(app, machineId);
        }
        Set<String> failedSet = new HashSet<>();
        try {
            ClusterGroupEntity entity = clusterConfigService.getClusterUniversalStateForAppMachine(app, machineId)
                .get(10, TimeUnit.SECONDS);
            Set<String> toModifySet = new HashSet<>();
            toModifySet.add(machineId);
            if (entity.getClientSet() != null) {
                toModifySet.addAll(entity.getClientSet());
            }
            // Modify mode to NOT-STARTED for all chosen token servers and associated token clients.
            modifyToNonStarted(toModifySet, failedSet);
        } catch (Exception ex) {
            Throwable e = ex instanceof ExecutionException ? ex.getCause() : ex;
            LOGGER.error("Failed to unbind machine <{}>", machineId, e);
            failedSet.add(machineId);
        }
        return new ClusterAppAssignResultVO()
            .setFailedClientSet(failedSet)
            .setFailedServerSet(new HashSet<>());
    }

    /**
     * Unbinds each machine in the set one by one and merges the failed sets of all
     * individual unbind operations into one result.
     */
    @Override
    public ClusterAppAssignResultVO unbindClusterServers(String app, Set<String> machineIdSet) {
        AssertUtil.assertNotBlank(app, "app cannot be blank");
        AssertUtil.isTrue(machineIdSet != null && !machineIdSet.isEmpty(), "machineIdSet cannot be empty");
        ClusterAppAssignResultVO result = new ClusterAppAssignResultVO()
            .setFailedClientSet(new HashSet<>())
            .setFailedServerSet(new HashSet<>());
        for (String machineId : machineIdSet) {
            ClusterAppAssignResultVO resultVO = unbindClusterServer(app, machineId);
            result.getFailedClientSet().addAll(resultVO.getFailedClientSet());
            result.getFailedServerSet().addAll(resultVO.getFailedServerSet());
        }
        return result;
    }

    /**
     * Applies the cluster assignment map to the app in three steps: entries belonging
     * to the app are switched to token-server mode and receive transport/flow/namespace
     * config; the client set of every entry is switched to token-client mode pointing
     * at that entry's server; the remaining unassigned machines are reset to NOT-STARTED.
     * Failed server IDs and client IDs are collected in the result.
     */
    @Override
    public ClusterAppAssignResultVO applyAssignToApp(String app, List<ClusterAppAssignMap> clusterMap,
                                                     Set<String> remainingSet) {
        AssertUtil.assertNotBlank(app, "app cannot be blank");
        AssertUtil.notNull(clusterMap, "clusterMap cannot be null");
        Set<String> failedServerSet = new HashSet<>();
        Set<String> failedClientSet = new HashSet<>();

        // Assign server and apply config.
        clusterMap.stream()
            .filter(Objects::nonNull)
            .filter(ClusterAppAssignMap::getBelongToApp)
            .map(e -> {
                String ip = e.getIp();
                int commandPort = parsePort(e);
                CompletableFuture<Void> f = modifyMode(ip, commandPort, ClusterStateManager.CLUSTER_SERVER)
                    .thenCompose(v -> applyServerConfigChange(app, ip, commandPort, e));
                return Tuple2.of(e.getMachineId(), f);
            })
            .forEach(t -> handleFutureSync(t, failedServerSet));

        // Assign client of servers and apply config.
        clusterMap.parallelStream()
            .filter(Objects::nonNull)
            .forEach(e -> applyAllClientConfigChange(app, e, failedClientSet));

        // Unbind remaining (unassigned) machines.
        applyAllRemainingMachineSet(app, remainingSet, failedClientSet);

        return new ClusterAppAssignResultVO()
            .setFailedClientSet(failedClientSet)
            .setFailedServerSet(failedServerSet);
    }

    /**
     * Resets all remaining (unassigned) machines in {@code remainingSet} to NOT-STARTED
     * mode in parallel, recording failures in {@code failedSet}; does nothing when the
     * set is null or empty.
     */
    private void applyAllRemainingMachineSet(String app, Set<String> remainingSet, Set<String> failedSet) {
        if (remainingSet == null || remainingSet.isEmpty()) {
            return;
        }
        remainingSet.parallelStream()
            .filter(Objects::nonNull)
            .map(MachineUtils::parseCommandIpAndPort)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ipPort -> {
                String ip = ipPort.r1;
                int commandPort = ipPort.r2;
                CompletableFuture<Void> f = modifyMode(ip, commandPort, ClusterStateManager.CLUSTER_NOT_STARTED);
                return Tuple2.of(ip + '@' + commandPort, f);
            })
            .forEach(t -> handleFutureSync(t, failedSet));
    }

    /**
     * Switches every machine in the assign map's client set to token-client mode and
     * points it at the assign map's server (with a fixed 20ms request timeout),
     * recording failures in {@code failedSet}.
     */
    private void applyAllClientConfigChange(String app, ClusterAppAssignMap assignMap,
                                            Set<String> failedSet) {
        Set<String> clientSet = assignMap.getClientSet();
        if (clientSet == null || clientSet.isEmpty()) {
            return;
        }
        final String serverIp = assignMap.getIp();
        final int serverPort = assignMap.getPort();
        clientSet.stream()
            .map(MachineUtils::parseCommandIpAndPort)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ipPort -> {
                CompletableFuture<Void> f = sentinelApiClient
                    .modifyClusterMode(ipPort.r1, ipPort.r2, ClusterStateManager.CLUSTER_CLIENT)
                    .thenCompose(v -> sentinelApiClient.modifyClusterClientConfig(app, ipPort.r1, ipPort.r2,
                        new ClusterClientConfig().setRequestTimeout(20)
                            .setServerHost(serverIp)
                            .setServerPort(serverPort)
                    ));
                return Tuple2.of(ipPort.r1 + '@' + ipPort.r2, f);
            })
            .forEach(t -> handleFutureSync(t, failedSet));
    }

    /**
     * Waits up to 10 seconds for the given future to complete; on failure logs the
     * error and records the machine ID in {@code failedSet}.
     */
    private void handleFutureSync(Tuple2<String, CompletableFuture<Void>> t, Set<String> failedSet) {
        try {
            t.r2.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex instanceof ExecutionException) {
                LOGGER.error("Request for <{}> failed", t.r1, ex.getCause());
            } else {
                LOGGER.error("Request for <{}> failed", t.r1, ex);
            }
            failedSet.add(t.r1);
        }
    }

    /**
     * Applies the token server config of an assigned server in sequence: transport
     * config (with a fixed 600s idle time), then flow config, then namespace set.
     */
    private CompletableFuture<Void> applyServerConfigChange(String app, String ip, int commandPort,
                                                            ClusterAppAssignMap assignMap) {
        ServerTransportConfig transportConfig = new ServerTransportConfig()
            .setPort(assignMap.getPort())
            .setIdleSeconds(600);
        return sentinelApiClient.modifyClusterServerTransportConfig(app, ip, commandPort, transportConfig)
            .thenCompose(v -> applyServerFlowConfigChange(app, ip, commandPort, assignMap))
            .thenCompose(v -> applyServerNamespaceSetConfig(app, ip, commandPort, assignMap));
    }

    /**
     * Applies the max allowed QPS flow config to the token server, skipped when the
     * value is absent, non-positive or greater than 200000.
     */
    private CompletableFuture<Void> applyServerFlowConfigChange(String app, String ip, int commandPort,
                                                                ClusterAppAssignMap assignMap) {
        Double maxAllowedQps = assignMap.getMaxAllowedQps();
        if (maxAllowedQps == null || maxAllowedQps <= 0 || maxAllowedQps > 20_0000) {
            return CompletableFuture.completedFuture(null);
        }
        return sentinelApiClient.modifyClusterServerFlowConfig(app, ip, commandPort,
            new ServerFlowConfig().setMaxAllowedQps(maxAllowedQps));
    }

    /**
     * Applies the namespace set to the token server, skipped when it is null or empty.
     */
    private CompletableFuture<Void> applyServerNamespaceSetConfig(String app, String ip, int commandPort,
                                                                  ClusterAppAssignMap assignMap) {
        Set<String> namespaceSet = assignMap.getNamespaceSet();
        if (namespaceSet == null || namespaceSet.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return sentinelApiClient.modifyClusterServerNamespaceSet(app, ip, commandPort, namespaceSet);
    }

    /** Modifies the cluster mode of the machine reachable at the given command port. */
    private CompletableFuture<Void> modifyMode(String ip, int port, int mode) {
        return sentinelApiClient.modifyClusterMode(ip, port, mode);
    }

    /**
     * Parses the command port from the assign map's machine ID, falling back to
     * {@link ServerTransportConfig#DEFAULT_PORT} when it cannot be parsed.
     */
    private int parsePort(ClusterAppAssignMap assignMap) {
        return MachineUtils.parseCommandPort(assignMap.getMachineId())
            .orElse(ServerTransportConfig.DEFAULT_PORT);
    }
}
