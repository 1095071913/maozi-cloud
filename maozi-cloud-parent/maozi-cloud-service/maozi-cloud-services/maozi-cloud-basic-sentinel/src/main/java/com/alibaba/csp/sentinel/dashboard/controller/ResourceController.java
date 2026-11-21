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
package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.command.vo.NodeVo;
import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.domain.ResourceTreeNode;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.vo.ResourceVO;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for fetching realtime statistics of a machine.
 *
 * @author Carpenter Lee
 * @date 2026/11/22
 */
@RestController
@RequestMapping(value = "/resource")
public class ResourceController {

    /** The logger of this controller. */
    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

    /** The client used to fetch resource node info from Sentinel clients via HTTP. */
    private final SentinelApiClient httpFetcher;

    public ResourceController(SentinelApiClient httpFetcher) {
        this.httpFetcher = httpFetcher;
    }

    /**
     * Fetches real time statistics info of the machine.
     *
     * @param ip        ip to fetch
     * @param port      port of the ip
     * @param type      one of [root, default, cluster], 'root' means fetching from tree root node, 'default' means
     *                  fetching from tree default node, 'cluster' means fetching from cluster node.
     * @param searchKey key to search
     * @return node statistics info.
     */
    @GetMapping("/machineResource.json")
    public Result<List<ResourceVO>> fetchResourceChainListOfMachine(String ip, Integer port, String type,
                                                                    String searchKey) {
        if (StringUtil.isEmpty(ip) || port == null) {
            return Result.ofFail(Result.CODE_ERROR, "invalid param, give ip, port");
        }
        // Local constants for the supported node types, lower camelCase as local variables.
        final String typeRoot = "root";
        final String typeDefault = "default";
        if (StringUtil.isEmpty(type)) {
            type = typeRoot;
        }
        if (typeRoot.equalsIgnoreCase(type) || typeDefault.equalsIgnoreCase(type)) {
            List<NodeVo> nodeVos = httpFetcher.fetchResourceOfMachine(ip, port, type);
            ResourceTreeNode treeNode = ResourceTreeNode.fromNodeVoList(nodeVos);
            if (treeNode == null) {
                return Result.ofSuccess(null);
            }
            treeNode.searchIgnoreCase(searchKey);
            return Result.ofSuccess(ResourceVO.fromResourceTreeNode(treeNode));
        } else {
            // Normal (cluster node).
            List<NodeVo> nodeVos = httpFetcher.fetchClusterNodeOfMachine(ip, port, true);
            if (StringUtil.isNotEmpty(searchKey)) {
                nodeVos = nodeVos.stream().filter(node -> node.getResource()
                    .toLowerCase().contains(searchKey.toLowerCase()))
                    .collect(Collectors.toList());
            }
            return Result.ofSuccess(ResourceVO.fromNodeVoList(nodeVos));
        }
    }
}
