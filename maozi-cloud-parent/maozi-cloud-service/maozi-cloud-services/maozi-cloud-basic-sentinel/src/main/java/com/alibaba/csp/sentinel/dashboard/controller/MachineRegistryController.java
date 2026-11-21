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

import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.apache.http.conn.util.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 接收 Sentinel 客户端心跳请求并注册或更新对应机器的控制器。
 * @author maozi
 * @date 2026/11/22
 */
@Controller
@RequestMapping(value = "/registry", produces = MediaType.APPLICATION_JSON_VALUE)
public class MachineRegistryController {

    /** 日志记录器。 */
    private final Logger logger = LoggerFactory.getLogger(MachineRegistryController.class);

    /** 维护已注册机器的应用管理服务。 */
    private final AppManagement appManagement;

    /** 上报应用名的最大允许长度。 */
    private static final int MAX_APP_NAME_LENGTH = 256;
    /** 上报 IP 地址字符串的最大允许长度。 */
    private static final int MAX_IP_LENGTH = 128;
    /** 上报主机名的最大允许长度。 */
    private static final int MAX_HOSTNAME_LENGTH = 256;

    /** 注入应用管理服务。 */
    public MachineRegistryController(AppManagement appManagement) {
        this.appManagement = appManagement;
    }

    /**
     * 接收 Sentinel 客户端心跳：校验上报的 app/ip/port 后注册或更新机器。
     * port 为 -1 表示客户端端口尚未设置，此时拒绝心跳并返回错误结果。
     */
    @ResponseBody
    @PostMapping("/machine")
    public Result<?> receiveHeartBeat(String app,
                                      @RequestParam(value = "app_type", required = false, defaultValue = "0")
                                          Integer appType, Long version, String v, String hostname, String ip,
                                      Integer port) {
        if (StringUtil.isBlank(app) || app.length() > MAX_APP_NAME_LENGTH) {
            return Result.ofFail(Result.CODE_ERROR, "invalid appName");
        }
        if (StringUtil.isBlank(ip) || ip.length() > MAX_IP_LENGTH) {
            return Result.ofFail(Result.CODE_ERROR, "invalid ip: " + ip);
        }
        if (!InetAddressUtils.isIPv4Address(ip) && !InetAddressUtils.isIPv6Address(ip)) {
            return Result.ofFail(Result.CODE_ERROR, "invalid ip: " + ip);
        }
        if (port == null || port < -1) {
            return Result.ofFail(Result.CODE_ERROR, "invalid port");
        }
        if (hostname != null && hostname.length() > MAX_HOSTNAME_LENGTH) {
            return Result.ofFail(Result.CODE_ERROR, "hostname too long");
        }
        if (port == -1) {
            logger.warn("Receive heartbeat from {} but port not set yet", ip);
            return Result.ofFail(Result.CODE_ERROR, "your port not set yet");
        }
        String sentinelVersion = StringUtil.isBlank(v) ? "unknown" : v;

        version = version == null ? System.currentTimeMillis() : version;
        try {
            MachineInfo machineInfo = new MachineInfo();
            machineInfo.setApp(app);
            machineInfo.setAppType(appType);
            machineInfo.setHostname(hostname);
            machineInfo.setIp(ip);
            machineInfo.setPort(port);
            machineInfo.setHeartbeatVersion(version);
            machineInfo.setLastHeartbeat(System.currentTimeMillis());
            machineInfo.setVersion(sentinelVersion);
            appManagement.addMachine(machineInfo);
            return Result.ofSuccessMsg(Result.DEFAULT_SUCCESS_MSG);
        } catch (Exception e) {
            logger.error("Receive heartbeat error", e);
            return Result.ofFail(Result.CODE_ERROR, e.getMessage());
        }
    }
}
