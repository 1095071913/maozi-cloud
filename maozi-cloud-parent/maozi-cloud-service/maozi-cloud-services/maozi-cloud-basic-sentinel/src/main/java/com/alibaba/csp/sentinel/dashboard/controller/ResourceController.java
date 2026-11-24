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
 * 获取机器实时统计信息的控制器。
 *
 * @author Carpenter Lee
 * @date 2026/11/22
 */
@RestController
@RequestMapping(value = "/resource")
public class ResourceController {

    /** 日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

    /** 节点类型：从树根节点获取。 */
    private static final String NODE_TYPE_ROOT = "root";
    /** 节点类型：从树默认节点获取。 */
    private static final String NODE_TYPE_DEFAULT = "default";

    /** 通过 HTTP 从 Sentinel 客户端拉取资源节点信息的客户端。 */
    private final SentinelApiClient httpFetcher;

    /** 注入 Sentinel API 客户端。 */
    public ResourceController(SentinelApiClient httpFetcher) {
        this.httpFetcher = httpFetcher;
    }

    /**
     * 获取机器的实时统计信息。
     *
     * @param ip        待查询的机器 IP
     * @param port      机器端口
     * @param type      节点类型，取值 [root, default, cluster]：root 从树根节点获取，
     *                  default 从树默认节点获取，cluster 从集群节点获取
     * @param searchKey 搜索关键字
     * @return 节点统计信息
     */
    @GetMapping("/machineResource.json")
    public Result<List<ResourceVO>> fetchResourceChainListOfMachine(String ip, Integer port, String type,
            String searchKey) {
        if (StringUtil.isEmpty(ip) || port == null) {
            return Result.ofFail(Result.CODE_ERROR, "invalid param, give ip, port");
        }
        if (StringUtil.isEmpty(type)) {
            type = NODE_TYPE_ROOT;
        }
        if (NODE_TYPE_ROOT.equalsIgnoreCase(type) || NODE_TYPE_DEFAULT.equalsIgnoreCase(type)) {
            List<NodeVo> nodeVos = httpFetcher.fetchResourceOfMachine(ip, port, type);
            ResourceTreeNode treeNode = ResourceTreeNode.fromNodeVoList(nodeVos);
            if (treeNode == null) {
                return Result.ofSuccess(null);
            }
            treeNode.searchIgnoreCase(searchKey);
            return Result.ofSuccess(ResourceVO.fromResourceTreeNode(treeNode));
        }
        // 其余类型按集群节点处理
        List<NodeVo> nodeVos = httpFetcher.fetchClusterNodeOfMachine(ip, port, true);
        if (StringUtil.isNotEmpty(searchKey)) {
            String lowerSearchKey = searchKey.toLowerCase();
            nodeVos = nodeVos.stream()
                    .filter(node -> node.getResource().toLowerCase().contains(lowerSearchKey))
                    .collect(Collectors.toList());
        }
        return Result.ofSuccess(ResourceVO.fromNodeVoList(nodeVos));
    }
}
