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

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.dashboard.client.CommandNotFoundException;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.SentinelVersion;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterClientModifyRequest;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterModifyRequest;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterServerModifyRequest;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.AppClusterClientStateWrapVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.AppClusterServerStateWrapVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStatePairVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterUniversalStateVO;
import com.alibaba.csp.sentinel.dashboard.service.ClusterConfigService;
import com.alibaba.csp.sentinel.dashboard.util.ClusterEntityUtils;
import com.alibaba.csp.sentinel.dashboard.util.VersionUtils;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * 查询与修改机器集群状态及配置的控制器。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
@RestController
@RequestMapping(value = "/cluster")
public class ClusterConfigController {

    /** 客户端版本不支持集群流控时返回的错误码。 */
    private static final int CODE_UNSUPPORTED_CLIENT = 4041;

    /** 修改请求报文中集群模式字段的 JSON 字段名。 */
    private static final String KEY_MODE = "mode";

    /** 支持集群流控的最低 Sentinel 客户端版本（1.4.0）。 */
    private static final SentinelVersion MIN_SUPPORTED_VERSION =
            new SentinelVersion().setMajorVersion(1).setMinorVersion(4);

    /** 日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(ClusterConfigController.class);

    /** 用于查询应用机器的应用管理服务。 */
    private final AppManagement appManagement;

    /** 拉取集群状态与修改集群配置的服务。 */
    private final ClusterConfigService clusterConfigService;

    /** 注入应用管理与集群配置服务。 */
    public ClusterConfigController(AppManagement appManagement, ClusterConfigService clusterConfigService) {
        this.appManagement = appManagement;
        this.clusterConfigService = clusterConfigService;
    }

    /**
     * 修改单个机器的集群配置：原始 JSON 报文按 "mode" 字段分发为客户端或
     * 服务端 {@link ClusterModifyRequest} 后应用。
     */
    @PostMapping("/config/modify_single")
    public Result<Boolean> apiModifyClusterConfig(@RequestBody String payload) {
        if (StringUtil.isBlank(payload)) {
            return Result.ofFail(Result.CODE_ERROR, "empty request body");
        }
        try {
            JSONObject body = JSON.parseObject(payload);
            if (body.containsKey(KEY_MODE)) {
                int mode = body.getInteger(KEY_MODE);
                switch (mode) {
                    case ClusterStateManager.CLUSTER_CLIENT:
                        ClusterClientModifyRequest clientRequest =
                                JSON.parseObject(payload, ClusterClientModifyRequest.class);
                        Result<Boolean> clientCheckResult = checkValidRequest(clientRequest);
                        if (clientCheckResult != null) {
                            return clientCheckResult;
                        }
                        clusterConfigService.modifyClusterClientConfig(clientRequest).get();
                        return Result.ofSuccess(true);
                    case ClusterStateManager.CLUSTER_SERVER:
                        ClusterServerModifyRequest serverRequest =
                                JSON.parseObject(payload, ClusterServerModifyRequest.class);
                        Result<Boolean> serverCheckResult = checkValidRequest(serverRequest);
                        if (serverCheckResult != null) {
                            return serverCheckResult;
                        }
                        // TODO maozi 2026/11/21 此处设计不佳，待重构
                        clusterConfigService.modifyClusterServerConfig(serverRequest).get();
                        return Result.ofSuccess(true);
                    default:
                        return Result.ofFail(Result.CODE_ERROR, "invalid mode");
                }
            }
            return Result.ofFail(Result.CODE_ERROR, "invalid parameter");
        } catch (ExecutionException ex) {
            logger.error("Error when modifying cluster config", ex.getCause());
            return errorResponse(ex);
        } catch (InterruptedException ex) {
            return onInterrupted(logger, "Error when modifying cluster config", ex);
        } catch (Exception ex) {
            logger.error("Error when modifying cluster config", ex);
            return Result.ofFail(Result.CODE_ERROR, ex.getMessage());
        }
    }

    /** 将 ExecutionException 转换为版本不支持结果或原因异常对应的错误结果。 */
    private <T> Result<T> errorResponse(ExecutionException ex) {
        if (isNotSupported(ex.getCause())) {
            return unsupportedVersion();
        }
        return Result.ofThrowable(Result.CODE_ERROR, ex.getCause());
    }

    /** 获取应用指定单台机器的通用集群状态。 */
    @GetMapping("/state_single")
    public Result<ClusterUniversalStateVO> apiGetClusterState(@RequestParam String app,
            @RequestParam String ip,
            @RequestParam Integer port) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app cannot be null or empty");
        }
        if (StringUtil.isEmpty(ip)) {
            return Result.ofFail(Result.CODE_ERROR, "ip cannot be null or empty");
        }
        if (port == null || port <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid parameter: port");
        }
        if (!checkIfSupported(app, ip, port)) {
            return unsupportedVersion();
        }
        try {
            return clusterConfigService.getClusterUniversalState(app, ip, port)
                    .thenApply(Result::ofSuccess)
                    .get();
        } catch (ExecutionException ex) {
            logger.error("Error when fetching cluster state", ex.getCause());
            return errorResponse(ex);
        } catch (InterruptedException ex) {
            return onInterrupted(logger, "Error when fetching cluster state", ex);
        } catch (Exception ex) {
            logger.error("Error when fetching cluster state", ex);
            return Result.ofFail(Result.CODE_ERROR, ex.getMessage());
        }
    }

    /** 获取应用全部机器的集群 server 状态。 */
    @GetMapping("/server_state/{app}")
    public Result<List<AppClusterServerStateWrapVO>> apiGetClusterServerStateOfApp(@PathVariable String app) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app cannot be null or empty");
        }
        try {
            return clusterConfigService.getClusterUniversalState(app)
                    .thenApply(ClusterEntityUtils::wrapToAppClusterServerState)
                    .thenApply(Result::ofSuccess)
                    .get();
        } catch (ExecutionException ex) {
            logger.error("Error when fetching cluster server state of app: {}", app, ex.getCause());
            return errorResponse(ex);
        } catch (InterruptedException ex) {
            return onInterrupted(logger, "Error when fetching cluster server state of app: " + app, ex);
        } catch (Exception ex) {
            logger.error("Error when fetching cluster server state of app: {}", app, ex);
            return Result.ofFail(Result.CODE_ERROR, ex.getMessage());
        }
    }

    /** 获取应用全部机器的集群 token client 状态。 */
    @GetMapping("/client_state/{app}")
    public Result<List<AppClusterClientStateWrapVO>> apiGetClusterClientStateOfApp(@PathVariable String app) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app cannot be null or empty");
        }
        try {
            return clusterConfigService.getClusterUniversalState(app)
                    .thenApply(ClusterEntityUtils::wrapToAppClusterClientState)
                    .thenApply(Result::ofSuccess)
                    .get();
        } catch (ExecutionException ex) {
            logger.error("Error when fetching cluster token client state of app: {}", app, ex.getCause());
            return errorResponse(ex);
        } catch (InterruptedException ex) {
            return onInterrupted(logger, "Error when fetching cluster token client state of app: " + app, ex);
        } catch (Exception ex) {
            logger.error("Error when fetching cluster token client state of app: {}", app, ex);
            return Result.ofFail(Result.CODE_ERROR, ex.getMessage());
        }
    }

    /** 获取应用全部机器的通用集群状态（server 与 client）。 */
    @GetMapping("/state/{app}")
    public Result<List<ClusterUniversalStatePairVO>> apiGetClusterStateOfApp(@PathVariable String app) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app cannot be null or empty");
        }
        try {
            return clusterConfigService.getClusterUniversalState(app)
                    .thenApply(Result::ofSuccess)
                    .get();
        } catch (ExecutionException ex) {
            logger.error("Error when fetching cluster state of app: {}", app, ex.getCause());
            return errorResponse(ex);
        } catch (InterruptedException ex) {
            return onInterrupted(logger, "Error when fetching cluster state of app: " + app, ex);
        } catch (Exception ex) {
            logger.error("Error when fetching cluster state of app: {}", app, ex);
            return Result.ofFail(Result.CODE_ERROR, ex.getMessage());
        }
    }

    /** 判断异常是否表示目标 Sentinel 客户端不支持该命令。 */
    private boolean isNotSupported(Throwable throwable) {
        return throwable instanceof CommandNotFoundException;
    }

    /**
     * 判断机器的客户端版本是否支持集群流控（>= 1.4.0）；
     * 机器信息不可用或出错时返回 true。
     */
    private boolean checkIfSupported(String app, String ip, int port) {
        try {
            return Optional.ofNullable(appManagement.getDetailApp(app))
                    .flatMap(appInfo -> appInfo.getMachine(ip, port))
                    .flatMap(machine -> VersionUtils.parseVersion(machine.getVersion())
                            .map(version -> version.greaterOrEqual(MIN_SUPPORTED_VERSION)))
                    .orElse(true);
        } catch (Exception ex) {
            // 出错或无法获取机器信息时返回 true
            return true;
        }
    }

    /** 校验修改请求（app/ip/port/mode 及版本支持）；返回首个错误结果，校验通过返回 null。 */
    private Result<Boolean> checkValidRequest(ClusterModifyRequest request) {
        String app = request.getApp();
        String ip = request.getIp();
        Integer port = request.getPort();
        Integer mode = request.getMode();
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app cannot be empty");
        }
        if (StringUtil.isEmpty(ip)) {
            return Result.ofFail(Result.CODE_ERROR, "ip cannot be empty");
        }
        if (port == null || port < 0) {
            return Result.ofFail(Result.CODE_ERROR, "invalid port");
        }
        if (mode == null || mode < 0) {
            return Result.ofFail(Result.CODE_ERROR, "invalid mode");
        }
        if (!checkIfSupported(app, ip, port)) {
            return unsupportedVersion();
        }
        return null;
    }

    /** 构造客户端版本不支持集群流控时的错误结果。 */
    private <R> Result<R> unsupportedVersion() {
        return Result.ofFail(CODE_UNSUPPORTED_CLIENT,
                "Sentinel client not supported for cluster flow control (unsupported version or dependency absent)");
    }

    /** 恢复中断标记，记录消息与异常并返回失败结果。 */
    public static <T> Result<T> onInterrupted(Logger logger, String message, InterruptedException ex) {
        Thread.currentThread().interrupt();
        logger.error(message, ex);
        return Result.ofFail(Result.CODE_ERROR, ex.getMessage());
    }

}
