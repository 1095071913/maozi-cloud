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
package com.alibaba.csp.sentinel.dashboard.rule;

import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认流控规则提供者：从最健康机器的 Sentinel 传输 API 拉取规则，
 * 而非从远端仓库拉取
 *
 * @author Eric Zhao
 * @date 2026/11/22
 */
@Component("flowRuleDefaultProvider")
public class FlowRuleApiProvider implements DynamicRuleProvider<List<FlowRuleEntity>> {

    /** 与机器 Sentinel 传输端点通信的客户端 */
    private final SentinelApiClient sentinelApiClient;
    /** 提供各应用机器信息的注册表 */
    private final AppManagement appManagement;

    /** 使用 Sentinel API 客户端与应用注册表创建提供者 */
    public FlowRuleApiProvider(SentinelApiClient sentinelApiClient, AppManagement appManagement) {
        this.sentinelApiClient = sentinelApiClient;
        this.appManagement = appManagement;
    }

    /**
     * 从应用的 Sentinel 客户端拉取当前流控规则。健康机器按最近心跳时间排序，
     * 从心跳最新的机器读取规则；应用名为空或无健康机器时返回空列表
     */
    @Override
    public List<FlowRuleEntity> getRules(String appName) throws Exception {
        if (StringUtil.isBlank(appName)) {
            return new ArrayList<>();
        }
        List<MachineInfo> list = appManagement.getDetailApp(appName).getMachines()
                .stream()
                .filter(MachineInfo::isHealthy)
                .sorted((e1, e2) -> Long.compare(e2.getLastHeartbeat(),
                        e1.getLastHeartbeat())).collect(Collectors.toList());
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        MachineInfo machine = list.get(0);
        return sentinelApiClient.fetchFlowRuleOfMachine(machine.getApp(), machine.getIp(), machine.getPort());
    }
}
