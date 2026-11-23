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
import com.alibaba.csp.sentinel.dashboard.repository.rule.AbstractInMemoryRuleRepository;
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
 * 流控规则控制器（v2）。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
@RestController
@RequestMapping(value = "/v2/flow")
public class FlowControllerV2 extends AbstractFlowRuleController {

    /** 注入规则仓储、规则提供者与发布者。 */
    public FlowControllerV2(AbstractInMemoryRuleRepository<FlowRuleEntity> repository,
                            @Qualifier("flowRuleNacosProvider") DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider,
                            @Qualifier("flowRuleNacosPublisher") DynamicRulePublisher<List<FlowRuleEntity>>
                                rulePublisher) {
        super(repository, ruleProvider, rulePublisher);
    }

    /**
     * 从 Nacos 拉取应用的流控规则，用集群流控 id 还原实体 id 后持久化并返回。
     */
    @GetMapping("/rules")
    @AuthAction(PrivilegeType.READ_RULE)
    public Result<List<FlowRuleEntity>> apiQueryMachineRules(@RequestParam String app) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }
        return super.fetchAndSaveRules(app, rules -> {
            for (FlowRuleEntity entity : rules) {
                entity.setApp(app);
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

    /**
     * 内联校验请求体与应用名（v2 规则不携带机器信息），再通过公共校验器校验
     * 流控规则特有字段；返回首个错误结果，校验通过返回 null。
     */
    @Override
    protected <R> Result<R> checkEntityInternal(FlowRuleEntity entity) {
        if (entity == null) {
            return Result.ofFail(Result.CODE_ERROR, "invalid body");
        }
        if (StringUtil.isBlank(entity.getApp())) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }
        return RuleEntityValidator.checkFlowRuleFields(entity);
    }

    /** 校验通过后新增流控规则（重置 id 以便生成），并推送应用的规则。 */
    @PostMapping("/rule")
    @AuthAction(value = AuthService.PrivilegeType.WRITE_RULE)
    public Result<FlowRuleEntity> apiAddFlowRule(@RequestBody FlowRuleEntity entity) {
        return super.addFlowRule(entity);
    }

    /**
     * 更新指定 id 的规则（app/ip/port/gmtCreate 沿用旧实体），并推送应用的规则。
     */
    @PutMapping("/rule/{id}")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<FlowRuleEntity> apiUpdateFlowRule(@PathVariable("id") Long id,
                                                    @RequestBody FlowRuleEntity entity) {
        if (id == null || id <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid id");
        }
        FlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofFail(Result.CODE_ERROR, "id " + id + " does not exist");
        }
        if (entity == null) {
            return Result.ofFail(Result.CODE_ERROR, "invalid body");
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
        return super.saveAndPublish(entity);
    }

    /** 删除指定 id 的规则并推送应用的规则；id 不存在时直接成功返回（数据为 null）。 */
    @DeleteMapping("/rule/{id}")
    @AuthAction(PrivilegeType.DELETE_RULE)
    public Result<Long> apiDeleteRule(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid id");
        }
        return super.deleteAndPublish(id);
    }
}
