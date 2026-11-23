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
package com.alibaba.csp.sentinel.dashboard.controller.cluster;

import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterAppAssignResultVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterAppFullAssignRequest;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterAppSingleServerAssignRequest;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterAppAssignMap;
import com.alibaba.csp.sentinel.dashboard.service.ClusterAssignService;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 集群 token server 分配与解绑操作控制器。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
@RestController
@RequestMapping("/cluster/assign")
public class ClusterAssignController {

    /** 日志记录器。 */
    private final Logger logger = LoggerFactory.getLogger(ClusterAssignController.class);

    /** 为机器应用或解绑集群角色分配的服务。 */
    private final ClusterAssignService clusterAssignService;

    /** 注入集群分配服务。 */
    public ClusterAssignController(ClusterAssignService clusterAssignService) {
        this.clusterAssignService = clusterAssignService;
    }

    /** 将全量集群分配（server 映射及剩余 client 列表）应用到应用的全部机器。 */
    @PostMapping("/all_server/{app}")
    public Result<ClusterAppAssignResultVO> apiAssignAllClusterServersOfApp(@PathVariable String app,
                                                                            @RequestBody
                                                                                ClusterAppFullAssignRequest
                                                                                    assignRequest) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app cannot be null or empty");
        }
        if (assignRequest == null) {
            return Result.ofFail(Result.CODE_ERROR, "bad request body");
        }
        // 缓存集群分配映射与剩余机器列表，避免同一方法内重复调用
        List<ClusterAppAssignMap> clusterMap = assignRequest.getClusterMap();
        Set<String> remainingList = assignRequest.getRemainingList();
        if (clusterMap == null || remainingList == null) {
            return Result.ofFail(Result.CODE_ERROR, "bad request body");
        }
        try {
            return Result.ofSuccess(clusterAssignService.applyAssignToApp(app, clusterMap, remainingList));
        } catch (Throwable throwable) {
            logger.error("Error when assigning full cluster servers for app: {}", app, throwable);
            return Result.ofFail(Result.CODE_ERROR, throwable.getMessage());
        }
    }

    /** 应用单个集群 server 的分配，将其 clusterMap 条目包装为列表处理。 */
    @PostMapping("/single_server/{app}")
    public Result<ClusterAppAssignResultVO> apiAssignSingleClusterServersOfApp(@PathVariable String app,
                                                                               @RequestBody
                                                                                   ClusterAppSingleServerAssignRequest
                                                                                   assignRequest) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app cannot be null or empty");
        }
        if (assignRequest == null) {
            return Result.ofFail(Result.CODE_ERROR, "bad request body");
        }
        // 缓存集群分配映射，避免同一方法内重复调用
        ClusterAppAssignMap clusterMap = assignRequest.getClusterMap();
        if (clusterMap == null) {
            return Result.ofFail(Result.CODE_ERROR, "bad request body");
        }
        try {
            return Result.ofSuccess(clusterAssignService.applyAssignToApp(app, 
                Collections.singletonList(clusterMap),
                assignRequest.getRemainingList()));
        } catch (Throwable throwable) {
            logger.error("Error when assigning single cluster servers for app: {}", app, throwable);
            return Result.ofFail(Result.CODE_ERROR, throwable.getMessage());
        }
    }

    /** 按指定机器 id 解绑应用对应的集群 server。 */
    @PostMapping("/unbind_server/{app}")
    public Result<ClusterAppAssignResultVO> apiUnbindClusterServersOfApp(@PathVariable String app,
                                                                         @RequestBody Set<String> machineIds) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app cannot be null or empty");
        }
        if (machineIds == null || machineIds.isEmpty()) {
            return Result.ofFail(Result.CODE_ERROR, "bad request body");
        }
        try {
            return Result.ofSuccess(clusterAssignService.unbindClusterServers(app, machineIds));
        } catch (Throwable throwable) {
            logger.error("Error when unbinding cluster server {} for app <{}>", machineIds, app, throwable);
            return Result.ofFail(Result.CODE_ERROR, throwable.getMessage());
        }
    }
}
