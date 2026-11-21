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

import java.util.List;
import java.util.Set;

/**
 * 默认流控规则发布者：通过 Sentinel 传输 API 将规则推送到应用的每台健康机器
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
@Component("flowRuleDefaultPublisher")
public class FlowRuleApiPublisher implements DynamicRulePublisher<List<FlowRuleEntity>> {

    /** 与机器 Sentinel 传输端点通信的客户端 */
    private final SentinelApiClient sentinelApiClient;
    /** 提供各应用机器信息的注册表 */
    private final AppManagement appManagement;

    /** 使用 Sentinel API 客户端与应用注册表创建发布者 */
    public FlowRuleApiPublisher(SentinelApiClient sentinelApiClient, AppManagement appManagement) {
        this.sentinelApiClient = sentinelApiClient;
        this.appManagement = appManagement;
    }

    /**
     * 通过 Sentinel 传输 API 将给定流控规则推送到应用的每台健康机器。
     * 应用名为空或 rules 为 {@code null} 时不做任何操作
     */
    @Override
    public void publish(String app, List<FlowRuleEntity> rules) throws Exception {
        if (StringUtil.isBlank(app)) {
            return;
        }
        if (rules == null) {
            return;
        }
        Set<MachineInfo> set = appManagement.getDetailApp(app).getMachines();

        for (MachineInfo machine : set) {
            if (!machine.isHealthy()) {
                continue;
            }
            // TODO maozi 2026/11/21 解析推送结果
            sentinelApiClient.setFlowRuleOfMachine(app, machine.getIp(), machine.getPort(), rules);
        }
    }
}
