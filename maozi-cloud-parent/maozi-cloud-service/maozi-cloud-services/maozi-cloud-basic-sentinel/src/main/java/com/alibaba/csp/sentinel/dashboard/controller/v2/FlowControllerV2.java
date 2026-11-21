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
package com.alibaba.csp.sentinel.dashboard.controller.v2;

import com.alibaba.csp.sentinel.dashboard.auth.AuthAction;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.PrivilegeType;
import com.alibaba.csp.sentinel.dashboard.controller.AbstractFlowRuleController;
import com.alibaba.csp.sentinel.dashboard.controller.RuleEntityValidator;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * Flow rule controller (v2).
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
@RestController
@RequestMapping(value = "/v2/flow")
public class FlowControllerV2 extends AbstractFlowRuleController {

    public FlowControllerV2(InMemoryRuleRepositoryAdapter<FlowRuleEntity> repository,
                            @Qualifier("flowRuleNacosProvider") DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider,
                            @Qualifier("flowRuleNacosPublisher") DynamicRulePublisher<List<FlowRuleEntity>> rulePublisher) {
        super(repository, ruleProvider, rulePublisher);
    }

    /** Fetches the app's flow rules from Nacos, restoring the entity id from the cluster flow id, persists them and returns them. */
    @GetMapping("/rules")
    @AuthAction(PrivilegeType.READ_RULE)
    public Result<List<FlowRuleEntity>> apiQueryMachineRules(@RequestParam String app) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        return fetchAndSaveRules(app, rules -> {
            for (FlowRuleEntity entity : rules) {
                entity.setApp(app);
                // Cache the cluster config and flow id to avoid repeated calls within the same loop iteration.
                ClusterFlowConfig clusterConfig = entity.getClusterConfig();
                if (clusterConfig != null) {
                    Long flowId = clusterConfig.getFlowId();
                    if (flowId != null) {
                        entity.setId(flowId);
                    }
                }
            }
        });
    }

    /** Validates the body and app inline (v2 rules carry no machine info), then the flow-rule specific fields via the shared validator; returns the first error or null. */
    @Override
    protected <R> Result<R> checkEntityInternal(FlowRuleEntity entity) {
        if (entity == null) {
            return Result.ofFail(-1, "invalid body");
        }
        if (StringUtil.isBlank(entity.getApp())) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        return RuleEntityValidator.checkFlowRuleFields(entity);
    }

    /** Adds a new flow rule after validation (id reset for generation) and publishes the app's rules. */
    @PostMapping("/rule")
    @AuthAction(value = AuthService.PrivilegeType.WRITE_RULE)
    public Result<FlowRuleEntity> apiAddFlowRule(@RequestBody FlowRuleEntity entity) {
        return addFlowRule(entity);
    }

    /** Updates the rule of the given id (app/ip/port/gmtCreate inherited from the old entity) and publishes the app's rules. */
    @PutMapping("/rule/{id}")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<FlowRuleEntity> apiUpdateFlowRule(@PathVariable("id") Long id,
                                                    @RequestBody FlowRuleEntity entity) {
        if (id == null || id <= 0) {
            return Result.ofFail(-1, "Invalid id");
        }
        FlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofFail(-1, "id " + id + " does not exist");
        }
        if (entity == null) {
            return Result.ofFail(-1, "invalid body");
        }

        entity.setApp(oldEntity.getApp());
        entity.setIp(oldEntity.getIp());
        entity.setPort(oldEntity.getPort());
        Result<FlowRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }

        entity.setId(id);
        entity.setGmtCreate(oldEntity.getGmtCreate());
        entity.setGmtModified(new Date());
        return saveAndPublish(entity);
    }

    /** Deletes the rule of the given id and publishes the app's rules; an unknown id returns success with null data. */
    @DeleteMapping("/rule/{id}")
    @AuthAction(PrivilegeType.DELETE_RULE)
    public Result<Long> apiDeleteRule(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.ofFail(-1, "Invalid id");
        }
        return deleteAndPublish(id);
    }
}
