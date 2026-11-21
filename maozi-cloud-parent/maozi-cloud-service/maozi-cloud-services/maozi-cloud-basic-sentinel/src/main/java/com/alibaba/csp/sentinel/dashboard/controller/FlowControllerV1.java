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

import com.alibaba.csp.sentinel.dashboard.auth.AuthAction;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.PrivilegeType;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.AbstractInMemoryRuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * v1 流控规则 API 控制器，基于内存仓储与 Nacos 提供者/发布者实现。
 *
 * @author leyou
 * @author Eric Zhao
 * @date 2026/11/22
 */
@RestController
@RequestMapping(value = "/v1/flow")
public class FlowControllerV1 extends AbstractFlowRuleController {

    /** 用于校验机器与应用归属关系的应用管理服务。 */
    private final AppManagement appManagement;

    /** 注入规则仓储、应用管理、规则提供者与发布者。 */
    public FlowControllerV1(AbstractInMemoryRuleRepository<FlowRuleEntity> repository,
                            AppManagement appManagement,
                            @Qualifier("flowRuleNacosProvider") DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider,
                            @Qualifier("flowRuleNacosPublisher") DynamicRulePublisher<List<FlowRuleEntity>>
                                rulePublisher) {
        super(repository, ruleProvider, rulePublisher);
        this.appManagement = appManagement;
    }

    /** 从 Nacos 拉取应用的流控规则，为每条补充机器信息后持久化并返回。 */
    @GetMapping("/rules")
    @AuthAction(PrivilegeType.READ_RULE)
    public Result<List<FlowRuleEntity>> apiQueryMachineRules(@RequestParam String app,
                                                             @RequestParam String ip,
                                                             @RequestParam Integer port) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(ip)) {
            return Result.ofFail(Result.CODE_ERROR, "ip can't be null or empty");
        }
        if (port == null) {
            return Result.ofFail(Result.CODE_ERROR, "port can't be null");
        }
        if (!appManagement.isValidMachineOfApp(app, ip)) {
            return Result.ofFail(Result.CODE_ERROR, "given ip does not belong to given app");
        }
        return fetchAndSaveRules(app, rules -> {
            for (FlowRuleEntity entity : rules) {
                entity.setApp(app);
                entity.setIp(ip);
                entity.setPort(port);
            }
        });
    }

    /**
     * 先校验机器字段（app/ip/port 及机器与应用归属关系），再校验流控规则特有字段；
     * 返回首个错误结果，校验通过返回 null。
     */
    @Override
    protected <R> Result<R> checkEntityInternal(FlowRuleEntity entity) {
        Result<R> baseResult = RuleEntityValidator.checkBaseFields(entity, appManagement);
        if (baseResult != null) {
            return baseResult;
        }
        return RuleEntityValidator.checkFlowRuleFields(entity);
    }

    /** 校验通过后新增流控规则（重置 id 以便仓储生成），并推送应用的规则。 */
    @PostMapping("/rule")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<FlowRuleEntity> apiAddFlowRule(@RequestBody FlowRuleEntity entity) {
        return addFlowRule(entity);
    }

    /**
     * 更新指定 id 的规则：仅应用非空参数，随后将应用的规则推送至 Nacos。
     * strategy != 0 时必须提供 refResource；controlBehavior 为 1 / 2 时必须提供
     * warmUpPeriodSec / maxQueueingTimeMs。
     */
    @PutMapping("/save.json")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<FlowRuleEntity> apiUpdateFlowRule(Long id, String app,
                                                  String limitApp, String resource, Integer grade,
                                                  Double count, Integer strategy, String refResource,
                                                  Integer controlBehavior, Integer warmUpPeriodSec,
                                                  Integer maxQueueingTimeMs) {
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }
        FlowRuleEntity entity = repository.findById(id);
        if (entity == null) {
            return Result.ofFail(Result.CODE_ERROR, "id " + id + " dose not exist");
        }
        if (StringUtil.isNotBlank(app)) {
            entity.setApp(app.trim());
        }
        if (StringUtil.isNotBlank(limitApp)) {
            entity.setLimitApp(limitApp.trim());
        }
        if (StringUtil.isNotBlank(resource)) {
            entity.setResource(resource.trim());
        }
        if (grade != null) {
            if (grade != RuleConstant.FLOW_GRADE_THREAD && grade != RuleConstant.FLOW_GRADE_QPS) {
                return Result.ofFail(Result.CODE_ERROR, "grade must be 0 or 1, but " + grade + " got");
            }
            entity.setGrade(grade);
        }
        if (count != null) {
            entity.setCount(count);
        }
        if (strategy != null) {
            if (strategy != RuleConstant.STRATEGY_DIRECT && strategy != RuleConstant.STRATEGY_RELATE
                && strategy != RuleConstant.STRATEGY_CHAIN) {
                return Result.ofFail(Result.CODE_ERROR, "strategy must be in [0, 1, 2], but " + strategy + " got");
            }
            entity.setStrategy(strategy);
            if (strategy != RuleConstant.STRATEGY_DIRECT) {
                if (StringUtil.isBlank(refResource)) {
                    return Result.ofFail(Result.CODE_ERROR, "refResource can't be null or empty when strategy!=0");
                }
                entity.setRefResource(refResource.trim());
            }
        }
        if (controlBehavior != null) {
            if (controlBehavior != RuleConstant.CONTROL_BEHAVIOR_DEFAULT
                && controlBehavior != RuleConstant.CONTROL_BEHAVIOR_WARM_UP
                && controlBehavior != RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER) {
                return Result.ofFail(Result.CODE_ERROR,
                    "controlBehavior must be in [0, 1, 2], but " + controlBehavior + " got");
            }
            if (controlBehavior == RuleConstant.CONTROL_BEHAVIOR_WARM_UP && warmUpPeriodSec == null) {
                return Result.ofFail(Result.CODE_ERROR, "warmUpPeriodSec can't be null when controlBehavior==1");
            }
            if (controlBehavior == RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER && maxQueueingTimeMs == null) {
                return Result.ofFail(Result.CODE_ERROR, "maxQueueingTimeMs can't be null when controlBehavior==2");
            }
            entity.setControlBehavior(controlBehavior);
            if (warmUpPeriodSec != null) {
                entity.setWarmUpPeriodSec(warmUpPeriodSec);
            }
            if (maxQueueingTimeMs != null) {
                entity.setMaxQueueingTimeMs(maxQueueingTimeMs);
            }
        }
        entity.setGmtModified(new Date());
        return saveAndPublish(entity);
    }

    /** 删除指定 id 的规则并推送应用的规则；id 不存在时直接成功返回（数据为 null）。 */
    @DeleteMapping("/delete.json")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<Long> apiDeleteFlowRule(Long id) {
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }
        return deleteAndPublish(id);
    }
}
