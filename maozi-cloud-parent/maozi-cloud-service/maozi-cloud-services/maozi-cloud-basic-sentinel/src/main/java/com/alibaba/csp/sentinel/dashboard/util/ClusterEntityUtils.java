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
import java.util.Objects;
import java.util.Set;

/**
 * 将集群通用状态 VO 封装为应用级服务端/客户端状态与集群分组的工具类
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public final class ClusterEntityUtils {

    /** 地址（ip:port）中 ip 与端口之间的分隔符 */
    private static final String ADDRESS_SEPARATOR = ":";

    /**
     * 将机器状态封装为应用级 Token 服务端状态：先处理运行在本应用机器上的
     * Token 服务端（含连接数），再处理从客户端配置推断出的外部 Token 服务端
     * （标记为不属于本应用）
     */
    public static List<AppClusterServerStateWrapVO> wrapToAppClusterServerState(
            List<ClusterUniversalStatePairVO> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, AppClusterServerStateWrapVO> map = new HashMap<>(list.size());
        Set<String> tokenServerSet = new HashSet<>(list.size());
        // 处理属于本应用的 Token 服务端
        for (ClusterUniversalStatePairVO stateVO : list) {
            ClusterUniversalStateVO state = stateVO.getState();
            Integer mode = state.getStateInfo().getMode();

            if (Objects.equals(mode, ClusterStateManager.CLUSTER_SERVER)) {
                String ip = stateVO.getIp();
                String serverId = ip + MachineUtils.MACHINE_ID_SEPARATOR + stateVO.getCommandPort();
                ClusterServerStateVO serverStateVO = state.getServer();
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
                tokenServerSet.add(ip + ADDRESS_SEPARATOR + serverStatePort);
            }
        }
        // 处理来自其他应用的 Token 服务端
        for (ClusterUniversalStatePairVO stateVO : list) {
            ClusterUniversalStateVO state = stateVO.getState();
            Integer mode = state.getStateInfo().getMode();

            if (Objects.equals(mode, ClusterStateManager.CLUSTER_CLIENT)) {
                ClusterClientStateVO clientState = state.getClient();
                if (clientState == null) {
                    continue;
                }
                ClusterClientInfoVO clientConfig = clientState.getClientConfig();
                if (clientConfig == null) {
                    continue;
                }
                String serverIp = clientConfig.getServerHost();
                Integer serverPort = clientConfig.getServerPort();
                // 外部 Token 服务端地址缺失或非法时跳过
                if (StringUtil.isBlank(serverIp) || serverPort == null || serverPort <= 0) {
                    continue;
                }
                if (tokenServerSet.contains(serverIp + ADDRESS_SEPARATOR + serverPort)) {
                    continue;
                }
                // 无法直接获取外部 Token 服务端的命令端口
                String serverId = serverIp + ADDRESS_SEPARATOR + serverPort;
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
     * 将机器状态封装为应用级 Token 客户端状态，
     * 每台运行在 Token 客户端模式的机器生成一个条目
     */
    public static List<AppClusterClientStateWrapVO> wrapToAppClusterClientState(
            List<ClusterUniversalStatePairVO> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, AppClusterClientStateWrapVO> map = new HashMap<>(list.size());
        for (ClusterUniversalStatePairVO stateVO : list) {
            ClusterUniversalStateVO state = stateVO.getState();
            Integer mode = state.getStateInfo().getMode();

            if (Objects.equals(mode, ClusterStateManager.CLUSTER_CLIENT)) {
                String ip = stateVO.getIp();
                Integer commandPort = stateVO.getCommandPort();
                String clientId = ip + MachineUtils.MACHINE_ID_SEPARATOR + commandPort;
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
     * 将机器状态按 Token 服务端地址分组为集群分组：每台服务端机器创建一个分组，
     * 随后把每个 Token 客户端加入其指向服务端的分组（服务端不在机器列表中时
     * 隐式创建分组）
     */
    public static List<ClusterGroupEntity> wrapToClusterGroup(List<ClusterUniversalStatePairVO> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, ClusterGroupEntity> map = new HashMap<>(list.size());
        for (ClusterUniversalStatePairVO stateVO : list) {
            ClusterUniversalStateVO state = stateVO.getState();
            Integer mode = state.getStateInfo().getMode();
            String ip = stateVO.getIp();
            if (Objects.equals(mode, ClusterStateManager.CLUSTER_SERVER)) {
                ClusterServerStateVO serverStateVO = state.getServer();
                if (serverStateVO == null) {
                    continue;
                }
                Integer port = serverStateVO.getPort();
                if (port == null || port <= 0) {
                    continue;
                }
                String serverAddress = getIp(ip);
                String targetAddress = serverAddress + ADDRESS_SEPARATOR + port;
                map.computeIfAbsent(targetAddress, v -> {
                    ClusterGroupEntity group = new ClusterGroupEntity();
                    group.setBelongToApp(true);
                    group.setMachineId(ip + MachineUtils.MACHINE_ID_SEPARATOR + stateVO.getCommandPort());
                    group.setIp(ip);
                    group.setPort(port);
                    return group;
                });
            }
        }
        for (ClusterUniversalStatePairVO stateVO : list) {
            ClusterUniversalStateVO state = stateVO.getState();
            Integer mode = state.getStateInfo().getMode();
            String ip = stateVO.getIp();
            if (Objects.equals(mode, ClusterStateManager.CLUSTER_CLIENT)) {
                ClusterClientStateVO targetClientState = state.getClient();
                if (targetClientState == null) {
                    continue;
                }
                ClusterClientInfoVO targetClientConfig = targetClientState.getClientConfig();
                if (targetClientConfig == null) {
                    continue;
                }
                String targetServer = targetClientConfig.getServerHost();
                Integer targetPort = targetClientConfig.getServerPort();
                if (StringUtil.isBlank(targetServer) || targetPort == null || targetPort <= 0) {
                    continue;
                }
                String targetAddress = targetServer + ADDRESS_SEPARATOR + targetPort;
                ClusterGroupEntity group = map.computeIfAbsent(targetAddress, v -> {
                    ClusterGroupEntity serverGroup = new ClusterGroupEntity();
                    serverGroup.setBelongToApp(true);
                    serverGroup.setMachineId(targetServer);
                    serverGroup.setIp(targetServer);
                    serverGroup.setPort(targetPort);
                    return serverGroup;
                });
                group.getClientSet().add(ip + MachineUtils.MACHINE_ID_SEPARATOR + stateVO.getCommandPort());
            }
        }
        return new ArrayList<>(map.values());
    }

    /** 返回 {@code ip:port} 字符串的 ip 部分，不含冒号时返回整个字符串 */
    private static String getIp(String str) {
        if (str.contains(ADDRESS_SEPARATOR)) {
            return str.split(ADDRESS_SEPARATOR)[0];
        }
        return str;
    }

    /** 私有构造方法，防止实例化 */
    private ClusterEntityUtils() {}
}
