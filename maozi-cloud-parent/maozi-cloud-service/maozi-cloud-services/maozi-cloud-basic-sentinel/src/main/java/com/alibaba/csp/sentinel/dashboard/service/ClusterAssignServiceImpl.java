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
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterClientInfoVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterGroupEntity;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerFlowConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterAppAssignMap;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStatePairVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStateVO;
import com.alibaba.csp.sentinel.dashboard.util.MachineUtils;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.function.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * {@link ClusterAssignService} 的默认实现：通过 Sentinel 命令 API
 * 为应用分配 Token 服务端与 Token 客户端，并解绑集群机器
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
@Service
public class ClusterAssignServiceImpl implements ClusterAssignService {

    /** 日志记录器 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterAssignServiceImpl.class);

    /** 对命令 API 的单次集群分配/读取操作的超时时间（秒） */
    private static final long CLUSTER_OP_TIMEOUT_SECONDS = 10L;

    /** 集群客户端配置使用的请求超时时间（毫秒） */
    private static final int CLIENT_REQUEST_TIMEOUT_MS = 20;

    /** 集群服务端传输配置使用的空闲时间（秒） */
    private static final int SERVER_IDLE_SECONDS = 600;

    /** 集群服务端最大允许 QPS 的上限（不含） */
    private static final double MAX_ALLOWED_QPS_LIMIT = 200_000D;

    /** 不属于本应用的 Token 服务端地址（ip:port）中 ip 与端口的分隔符 */
    private static final String NOT_IN_APP_ADDRESS_SEPARATOR = ":";

    /** 向机器发送集群模式/配置修改请求的客户端 */
    private final SentinelApiClient sentinelApiClient;
    /** 查询应用机器集群状态的服务 */
    private final ClusterConfigService clusterConfigService;

    /** 注入 Sentinel API 客户端与集群配置服务 */
    public ClusterAssignServiceImpl(SentinelApiClient sentinelApiClient,
            ClusterConfigService clusterConfigService) {
        this.sentinelApiClient = sentinelApiClient;
        this.clusterConfigService = clusterConfigService;
    }

    /**
     * 判断 machineId 是否为 {@code ip:port} 形式地址，该形式指未注册为
     * 本应用机器的 Token 服务端（应用内机器 ID 为 {@code ip@commandPort} 形式）。
     * 注意：尽管方法名为 isMachineInApp，返回 {@code true} 实际表示"不在应用内"
     */
    private boolean isMachineInApp(String machineId) {
        return machineId.contains(NOT_IN_APP_ADDRESS_SEPARATOR);
    }

    /**
     * 解绑不属于本应用机器的 Token 服务端（控制台无法直接访问它）：
     * 找出应用内客户端配置指向该 {@code ip:port} 服务端的全部 Token 客户端，
     * 将其重置为 NOT-STARTED 模式。失败项记入结果的失败客户端集合
     * （状态查询失败时记入服务端地址本身）
     */
    private ClusterAppAssignResultVO handleUnbindClusterServerNotInApp(String app, String machineId) {
        Set<String> failedSet = new HashSet<>();
        try {
            List<ClusterUniversalStatePairVO> list = clusterConfigService.getClusterUniversalState(app)
                    .get(CLUSTER_OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Set<String> toModifySet = list.stream()
                    .filter(pair -> {
                        ClusterUniversalStateVO state = pair.getState();
                        if (!Objects.equals(state.getStateInfo().getMode(), ClusterStateManager.CLUSTER_CLIENT)) {
                            return false;
                        }
                        ClusterClientInfoVO clientConfig = state.getClient().getClientConfig();
                        String serverAddress = clientConfig.getServerHost() + NOT_IN_APP_ADDRESS_SEPARATOR
                                + clientConfig.getServerPort();
                        return machineId.equals(serverAddress);
                    })
                    .map(pair -> pair.getIp() + MachineUtils.MACHINE_ID_SEPARATOR + pair.getCommandPort())
                    .collect(Collectors.toSet());
            // 将所有关联的 Token 客户端重置为 NOT-STARTED 模式
            modifyToNonStarted(toModifySet, failedSet);
        } catch (InterruptedException ex) {
            onInterrupted(machineId, "Failed to unbind machine <{}>", ex, failedSet);
        } catch (Exception ex) {
            Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
            LOGGER.error("Failed to unbind machine <{}>", machineId, cause);
            failedSet.add(machineId);
        }
        return new ClusterAppAssignResultVO()
                .setFailedClientSet(failedSet)
                .setFailedServerSet(new HashSet<>());
    }

    /**
     * 将 {@code toModifySet} 中的每台机器（ID 为 {@code ip@commandPort} 形式）
     * 并行重置为 NOT-STARTED 集群模式，失败项记入 {@code failedSet}
     */
    private void modifyToNonStarted(Set<String> toModifySet, Set<String> failedSet) {
        toModifySet.parallelStream()
                .map(MachineUtils::parseCommandIpAndPort)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ipPort -> {
                    CompletableFuture<Void> future = modifyMode(ipPort.r1, ipPort.r2,
                            ClusterStateManager.CLUSTER_NOT_STARTED);
                    return Tuple2.of(ipPort.r1 + MachineUtils.MACHINE_ID_SEPARATOR + ipPort.r2, future);
                })
                .forEach(futureTuple -> handleFutureSync(futureTuple, failedSet));
    }

    /**
     * 将指定机器及其名下全部 Token 客户端重置为 NOT-STARTED 模式以解绑该机器。
     * machineId 为外部 Token 服务端的 {@code ip:port} 地址时，仅重置其应用内的
     * Token 客户端。失败的机器 ID 记入结果的失败客户端集合
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
                    .get(CLUSTER_OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Set<String> toModifySet = new HashSet<>();
            toModifySet.add(machineId);
            // 缓存机器的 Token 客户端集合，避免同一方法内重复调用
            Set<String> clientSet = entity.getClientSet();
            if (clientSet != null) {
                toModifySet.addAll(clientSet);
            }
            // 将选中的 Token 服务端及其关联的 Token 客户端重置为 NOT-STARTED 模式
            modifyToNonStarted(toModifySet, failedSet);
        } catch (InterruptedException ex) {
            onInterrupted(machineId, "Failed to unbind machine <{}>", ex, failedSet);
        } catch (Exception ex) {
            Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
            LOGGER.error("Failed to unbind machine <{}>", machineId, cause);
            failedSet.add(machineId);
        }
        return new ClusterAppAssignResultVO()
                .setFailedClientSet(failedSet)
                .setFailedServerSet(new HashSet<>());
    }

    /**
     * 逐个解绑集合中的每台机器，并将各次解绑操作的失败集合合并为一个结果
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
     * 分三步将集群分配映射应用到该应用：属于本应用的条目切换为 Token 服务端模式
     * 并下发传输/流控/命名空间配置；每个条目的客户端集合切换为指向该条目服务端的
     * Token 客户端模式；剩余未分配的机器重置为 NOT-STARTED。
     * 失败的服务端 ID 与客户端 ID 记入结果
     */
    @Override
    public ClusterAppAssignResultVO applyAssignToApp(String app, List<ClusterAppAssignMap> clusterMap,
            Set<String> remainingSet) {
        AssertUtil.assertNotBlank(app, "app cannot be blank");
        AssertUtil.notNull(clusterMap, "clusterMap cannot be null");
        Set<String> failedServerSet = new HashSet<>();
        Set<String> failedClientSet = new HashSet<>();

        // 分配服务端并下发配置
        clusterMap.stream()
                .filter(Objects::nonNull)
                // belongToApp 为 Boolean 包装类型，请求体缺省时为 null，需判空避免拆箱 NPE
                .filter(assignMap -> Boolean.TRUE.equals(assignMap.getBelongToApp()))
                .map(assignMap -> {
                    String ip = assignMap.getIp();
                    int commandPort = parsePort(assignMap);
                    CompletableFuture<Void> future = modifyMode(ip, commandPort, ClusterStateManager.CLUSTER_SERVER)
                            .thenCompose(v -> applyServerConfigChange(app, ip, commandPort, assignMap));
                    return Tuple2.of(assignMap.getMachineId(), future);
                })
                .forEach(futureTuple -> handleFutureSync(futureTuple, failedServerSet));

        // 分配各服务端的客户端并下发配置
        clusterMap.parallelStream()
                .filter(Objects::nonNull)
                .forEach(assignMap -> applyAllClientConfigChange(app, assignMap, failedClientSet));

        // 解绑剩余（未分配的）机器
        applyAllRemainingMachineSet(remainingSet, failedClientSet);

        return new ClusterAppAssignResultVO()
                .setFailedClientSet(failedClientSet)
                .setFailedServerSet(failedServerSet);
    }

    /**
     * 将 {@code remainingSet} 中剩余（未分配的）机器并行重置为 NOT-STARTED 模式，
     * 失败项记入 {@code failedSet}；集合为 null 或空时不做任何操作
     */
    private void applyAllRemainingMachineSet(Set<String> remainingSet, Set<String> failedSet) {
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
                    CompletableFuture<Void> future =
                            modifyMode(ip, commandPort, ClusterStateManager.CLUSTER_NOT_STARTED);
                    return Tuple2.of(ip + MachineUtils.MACHINE_ID_SEPARATOR + commandPort, future);
                })
                .forEach(futureTuple -> handleFutureSync(futureTuple, failedSet));
    }

    /**
     * 将分配映射客户端集合中的每台机器切换为 Token 客户端模式并指向
     * 该映射的服务端（固定 20ms 请求超时），失败项记入 {@code failedSet}
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
                    CompletableFuture<Void> future = sentinelApiClient
                            .modifyClusterMode(ipPort.r1, ipPort.r2, ClusterStateManager.CLUSTER_CLIENT)
                            .thenCompose(v -> sentinelApiClient.modifyClusterClientConfig(app, ipPort.r1, ipPort.r2,
                                    new ClusterClientConfig().setRequestTimeout(CLIENT_REQUEST_TIMEOUT_MS)
                                            .setServerHost(serverIp)
                                            .setServerPort(serverPort)
                            ));
                    return Tuple2.of(ipPort.r1 + MachineUtils.MACHINE_ID_SEPARATOR + ipPort.r2, future);
                })
                .forEach(futureTuple -> handleFutureSync(futureTuple, failedSet));
    }

    /**
     * 最多等待 10 秒直到给定 Future 完成；失败时记录错误日志
     * 并将机器 ID 记入 {@code failedSet}
     */
    private void handleFutureSync(Tuple2<String, CompletableFuture<Void>> futureTuple, Set<String> failedSet) {
        try {
            futureTuple.r2.get(CLUSTER_OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            onInterrupted(futureTuple.r1, "Request for <{}> failed", ex, failedSet);
        } catch (Exception ex) {
            if (ex instanceof ExecutionException) {
                LOGGER.error("Request for <{}> failed", futureTuple.r1, ex.getCause());
            } else {
                LOGGER.error("Request for <{}> failed", futureTuple.r1, ex);
            }
            failedSet.add(futureTuple.r1);
        }
    }

    /**
     * 恢复当前线程的中断标记，按给定消息记录指定机器的失败日志，
     * 并将机器 ID 记入失败集合
     */
    private void onInterrupted(String machineId, String message, InterruptedException ex, Set<String> failedSet) {
        Thread.currentThread().interrupt();
        LOGGER.error(message, machineId, ex);
        failedSet.add(machineId);
    }

    /**
     * 依次为已分配的服务端下发 Token 服务端配置：传输配置
     * （固定 600 秒空闲时间）、流控配置、命名空间集合
     */
    private CompletableFuture<Void> applyServerConfigChange(String app, String ip, int commandPort,
            ClusterAppAssignMap assignMap) {
        ServerTransportConfig transportConfig = new ServerTransportConfig()
                .setPort(assignMap.getPort())
                .setIdleSeconds(SERVER_IDLE_SECONDS);
        return sentinelApiClient.modifyClusterServerTransportConfig(app, ip, commandPort, transportConfig)
                .thenCompose(v -> applyServerFlowConfigChange(app, ip, commandPort, assignMap))
                .thenCompose(v -> applyServerNamespaceSetConfig(app, ip, commandPort, assignMap));
    }

    /**
     * 向 Token 服务端下发最大允许 QPS 流控配置，
     * 值缺失、非正数或大于 200000 时跳过
     */
    private CompletableFuture<Void> applyServerFlowConfigChange(String app, String ip, int commandPort,
            ClusterAppAssignMap assignMap) {
        Double maxAllowedQps = assignMap.getMaxAllowedQps();
        if (maxAllowedQps == null || maxAllowedQps <= 0 || maxAllowedQps > MAX_ALLOWED_QPS_LIMIT) {
            return CompletableFuture.completedFuture(null);
        }
        return sentinelApiClient.modifyClusterServerFlowConfig(app, ip, commandPort,
                new ServerFlowConfig().setMaxAllowedQps(maxAllowedQps));
    }

    /**
     * 向 Token 服务端下发命名空间集合，集合为 null 或空时跳过
     */
    private CompletableFuture<Void> applyServerNamespaceSetConfig(String app, String ip, int commandPort,
            ClusterAppAssignMap assignMap) {
        Set<String> namespaceSet = assignMap.getNamespaceSet();
        if (namespaceSet == null || namespaceSet.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return sentinelApiClient.modifyClusterServerNamespaceSet(app, ip, commandPort, namespaceSet);
    }

    /** 修改指定命令端口机器的集群模式 */
    private CompletableFuture<Void> modifyMode(String ip, int port, int mode) {
        return sentinelApiClient.modifyClusterMode(ip, port, mode);
    }

    /**
     * 从分配映射的机器 ID 解析命令端口，解析失败时
     * 回退为 {@link ServerTransportConfig#DEFAULT_PORT}
     */
    private int parsePort(ClusterAppAssignMap assignMap) {
        return MachineUtils.parseCommandPort(assignMap.getMachineId())
                .orElse(ServerTransportConfig.DEFAULT_PORT);
    }
}
