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
package com.alibaba.csp.sentinel.dashboard.util;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterClientInfoVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterGroupEntity;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ConnectionGroupVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.AppClusterClientStateWrapVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.AppClusterServerStateWrapVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterClientStateVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterServerStateVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStatePairVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStateVO;
import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for wrapping cluster universal state VOs into app-level server/client
 * state and cluster group entities.
 *
 * @author Eric Zhao
 * @since 1.4.1
 */
public final class ClusterEntityUtils {

    /**
     * Wraps the machine states into app-level token server states: first the token
     * servers running on machines of the current app (with their connection counts),
     * then foreign token servers inferred from client configs (marked as not belonging
     * to the app).
     */
    public static List<AppClusterServerStateWrapVO> wrapToAppClusterServerState(
        List<ClusterUniversalStatePairVO> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, AppClusterServerStateWrapVO> map = new HashMap<>();
        Set<String> tokenServerSet = new HashSet<>();
        // Handle token servers that belong to current app.
        for (ClusterUniversalStatePairVO stateVO : list) {
            // Cache the state object to avoid repeated calls within the same iteration.
            ClusterUniversalStateVO state = stateVO.getState();
            int mode = state.getStateInfo().getMode();

            if (mode == ClusterStateManager.CLUSTER_SERVER) {
                String ip = stateVO.getIp();
                String serverId = ip + '@' + stateVO.getCommandPort();
                ClusterServerStateVO serverStateVO = state.getServer();
                // Cache the server port to avoid repeated calls within the same iteration.
                Integer serverStatePort = serverStateVO.getPort();
                map.computeIfAbsent(serverId, v -> new AppClusterServerStateWrapVO()
                    .setId(serverId)
                    .setIp(ip)
                    .setPort(serverStatePort)
                    .setState(serverStateVO)
                    .setBelongToApp(true)
                    .setConnectedCount(serverStateVO.getConnection().stream()
                        .mapToInt(ConnectionGroupVO::getConnectedCount)
                        .sum()
                    )
                );
                tokenServerSet.add(ip + ":" + serverStatePort);
            }
        }
        // Handle token servers from other app.
        for (ClusterUniversalStatePairVO stateVO : list) {
            // Cache the state object to avoid repeated calls within the same iteration.
            ClusterUniversalStateVO state = stateVO.getState();
            int mode = state.getStateInfo().getMode();

            if (mode == ClusterStateManager.CLUSTER_CLIENT) {
                ClusterClientStateVO clientState = state.getClient();
                if (clientState == null) {
                    continue;
                }
                // Cache the client config to avoid repeated calls within the same iteration.
                ClusterClientInfoVO clientConfig = clientState.getClientConfig();
                String serverIp = clientConfig.getServerHost();
                int serverPort = clientConfig.getServerPort();
                if (tokenServerSet.contains(serverIp + ":" + serverPort)) {
                    continue;
                }
                // We are not able to get the commandPort of foreign token server directly.
                String serverId = String.format("%s:%d", serverIp, serverPort);
                map.computeIfAbsent(serverId, v -> new AppClusterServerStateWrapVO()
                    .setId(serverId)
                    .setIp(serverIp)
                    .setPort(serverPort)
                    .setBelongToApp(false)
                );
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Wraps the machine states into app-level token client states, one entry per
     * machine running in token client mode.
     */
    public static List<AppClusterClientStateWrapVO> wrapToAppClusterClientState(
        List<ClusterUniversalStatePairVO> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, AppClusterClientStateWrapVO> map = new HashMap<>();
        for (ClusterUniversalStatePairVO stateVO : list) {
            // Cache the state object to avoid repeated calls within the same iteration.
            ClusterUniversalStateVO state = stateVO.getState();
            int mode = state.getStateInfo().getMode();

            if (mode == ClusterStateManager.CLUSTER_CLIENT) {
                String ip = stateVO.getIp();
                // Cache the command port to avoid repeated calls within the same iteration.
                Integer commandPort = stateVO.getCommandPort();
                String clientId = ip + '@' + commandPort;
                ClusterClientStateVO clientStateVO = state.getClient();
                map.computeIfAbsent(clientId, v -> new AppClusterClientStateWrapVO()
                    .setId(clientId)
                    .setIp(ip)
                    .setState(clientStateVO)
                    .setCommandPort(commandPort)
                );
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Groups the machine states into cluster groups keyed by token server address:
     * each server machine creates a group, then every token client is added to the
     * group of the server it points to (implicitly creating a group for servers that
     * are absent from the machine list).
     */
    public static List<ClusterGroupEntity> wrapToClusterGroup(List<ClusterUniversalStatePairVO> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, ClusterGroupEntity> map = new HashMap<>();
        for (ClusterUniversalStatePairVO stateVO : list) {
            // Cache the state object to avoid repeated calls within the same iteration.
            ClusterUniversalStateVO state = stateVO.getState();
            int mode = state.getStateInfo().getMode();
            String ip = stateVO.getIp();
            if (mode == ClusterStateManager.CLUSTER_SERVER) {
                String serverAddress = getIp(ip);
                int port = state.getServer().getPort();
                String targetAddress = serverAddress + ":" + port;
                map.computeIfAbsent(targetAddress, v -> {
                    ClusterGroupEntity group = new ClusterGroupEntity();
                    group.setBelongToApp(true);
                    group.setMachineId(ip + '@' + stateVO.getCommandPort());
                    group.setIp(ip);
                    group.setPort(port);
                    return group;
                });
            }
        }
        for (ClusterUniversalStatePairVO stateVO : list) {
            // Cache the state object to avoid repeated calls within the same iteration.
            ClusterUniversalStateVO state = stateVO.getState();
            int mode = state.getStateInfo().getMode();
            String ip = stateVO.getIp();
            if (mode == ClusterStateManager.CLUSTER_CLIENT) {
                // Cache the client config to avoid repeated calls within the same iteration.
                ClusterClientInfoVO targetClientConfig = state.getClient().getClientConfig();
                String targetServer = targetClientConfig.getServerHost();
                Integer targetPort = targetClientConfig.getServerPort();
                if (StringUtil.isBlank(targetServer) || targetPort == null || targetPort <= 0) {
                    continue;
                }
                String targetAddress = targetServer + ":" + targetPort;
                ClusterGroupEntity group = map.computeIfAbsent(targetAddress, v -> {
                    ClusterGroupEntity serverGroup = new ClusterGroupEntity();
                    serverGroup.setBelongToApp(true);
                    serverGroup.setMachineId(targetServer);
                    serverGroup.setIp(targetServer);
                    serverGroup.setPort(targetPort);
                    return serverGroup;
                });
                group.getClientSet().add(ip + '@' + stateVO.getCommandPort());
            }
        }
        return new ArrayList<>(map.values());
    }

    /** Returns the ip part of an {@code ip:port} string, or the whole string when it has no colon. */
    private static String getIp(String str) {
        if (str.contains(":")) {
            return str.split(":")[0];
        }
        return str;
    }

    /** Private constructor to prevent instantiation. */
    private ClusterEntityUtils() {}
}
