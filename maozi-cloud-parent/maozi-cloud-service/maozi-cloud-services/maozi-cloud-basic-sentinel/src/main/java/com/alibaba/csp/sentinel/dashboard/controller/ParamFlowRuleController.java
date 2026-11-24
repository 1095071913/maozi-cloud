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
import com.alibaba.csp.sentinel.dashboard.auth.AuthService;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.PrivilegeTypeEnum;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.SentinelVersion;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.RuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.dashboard.util.VersionUtils;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;

/**
 * 热点参数限流规则控制器。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
@RestController
@RequestMapping(value = "/paramFlow")
public class ParamFlowRuleController {

    /** 客户端版本不支持热点参数限流时返回的错误码。 */
    private static final int CODE_UNSUPPORTED_CLIENT = 4041;

    /** 支持热点参数限流的最低 Sentinel 客户端版本（0.2.x）。 */
    private static final SentinelVersion MIN_SUPPORTED_VERSION = new SentinelVersion().setMinorVersion(2);

    /** 日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(ParamFlowRuleController.class);

    /** 用于校验机器与应用归属关系的应用管理服务。 */
    private final AppManagement appManagement;
    /** 存储热点参数规则实体的内存仓储。 */
    private final RuleRepository<ParamFlowRuleEntity, Long> repository;

    /** 从 Nacos 拉取指定应用热点参数规则的提供者。 */
    private final DynamicRuleProvider<List<ParamFlowRuleEntity>> ruleProvider;
    /** 向指定应用推送热点参数规则的发布者。 */
    private final DynamicRulePublisher<List<ParamFlowRuleEntity>> rulePublisher;

    /** 注入应用管理、规则仓储、规则提供者与发布者。 */
    public ParamFlowRuleController(AppManagement appManagement, RuleRepository<ParamFlowRuleEntity, Long> repository,
            @Qualifier("paramFlowRuleNacosProvider") DynamicRuleProvider<List<ParamFlowRuleEntity>> ruleProvider,
            @Qualifier("paramFlowRuleNacosPublisher") DynamicRulePublisher<List<ParamFlowRuleEntity>> rulePublisher) {
        this.appManagement = appManagement;
        this.repository = repository;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /**
     * 判断机器的客户端版本是否支持热点参数限流（>= 0.2.0）；
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

    /** 从 Nacos 拉取应用的热点参数规则，补充机器信息后持久化并返回。 */
    @GetMapping("/rules")
    @AuthAction(PrivilegeTypeEnum.READ_RULE)
    public Result<List<ParamFlowRuleEntity>> apiQueryAllRulesForMachine(@RequestParam String app,
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
        if (!appManagement.isValidMachineOfApp(app, ip)) {
            return Result.ofFail(Result.CODE_ERROR, "given ip does not belong to given app");
        }
        if (!checkIfSupported(app, ip, port)) {
            return unsupportedVersion();
        }
        try {
            List<ParamFlowRuleEntity> rules = ruleProvider.getRules(app);
            for (ParamFlowRuleEntity entity : rules) {
                entity.setApp(app);
                entity.setIp(ip);
                entity.setPort(port);
            }
            rules = repository.saveAll(rules);
            return Result.ofSuccess(rules);
        } catch (Exception e) {
            logger.error("Error when querying parameter flow rules", e);
            return Result.ofFail(Result.CODE_ERROR, e.getMessage());
        }
    }

    /** 校验通过后新增规则，并将应用的规则推送至 Nacos。 */
    @PostMapping("/rule")
    @AuthAction(AuthService.PrivilegeTypeEnum.WRITE_RULE)
    public Result<ParamFlowRuleEntity> apiAddParamFlowRule(@RequestBody ParamFlowRuleEntity entity) {
        Result<ParamFlowRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }
        if (!checkIfSupported(entity.getApp(), entity.getIp(), entity.getPort())) {
            return unsupportedVersion();
        }
        entity.setId(null);
        entity.getRule().setResource(entity.getResource().trim());
        entity.setGmtCreateAndModified(new Date());
        try {
            entity = repository.save(entity);
            publishRules(entity.getApp());
            return Result.ofSuccess(entity);
        } catch (Exception e) {
            logger.error("Error when adding new parameter flow rules", e);
            return Result.ofFail(Result.CODE_ERROR, e.getMessage());
        }
    }

    /**
     * 先通过公共校验器校验基础机器字段，再校验热点参数规则特有字段；
     * 返回首个错误结果，校验通过返回 null。
     */
    private <R> Result<R> checkEntityInternal(ParamFlowRuleEntity entity) {
        if (entity == null) {
            return Result.ofFail(Result.CODE_ERROR, "bad rule body");
        }
        Result<R> baseResult = RuleEntityValidator.checkBaseFields(entity);
        if (baseResult != null) {
            return baseResult;
        }
        if (entity.getRule() == null) {
            return Result.ofFail(Result.CODE_ERROR, "rule can't be null");
        }
        if (StringUtil.isBlank(entity.getResource())) {
            return Result.ofFail(Result.CODE_ERROR, "resource name cannot be null or empty");
        }
        Double count = entity.getCount();
        if (count == null || count < 0) {
            return Result.ofFail(Result.CODE_ERROR, "count should be valid");
        }
        Integer grade = entity.getGrade();
        if (grade == null || grade != RuleConstant.FLOW_GRADE_QPS) {
            return Result.ofFail(Result.CODE_ERROR, "Unknown mode (blockGrade) for parameter flow control");
        }
        Integer paramIdx = entity.getParamIdx();
        if (paramIdx == null || paramIdx < 0) {
            return Result.ofFail(Result.CODE_ERROR, "paramIdx should be valid");
        }
        if (entity.getDurationInSec() <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "durationInSec should be valid");
        }
        Integer controlBehavior = entity.getControlBehavior();
        if (controlBehavior == null || controlBehavior < 0) {
            return Result.ofFail(Result.CODE_ERROR, "controlBehavior should be valid");
        }
        return null;
    }

    /** 更新指定 id 的规则（gmtCreate 沿用旧实体），并推送应用的规则。 */
    @PutMapping("/rule/{id}")
    @AuthAction(AuthService.PrivilegeTypeEnum.WRITE_RULE)
    public Result<ParamFlowRuleEntity> apiUpdateParamFlowRule(@PathVariable("id") Long id,
            @RequestBody ParamFlowRuleEntity entity) {
        if (id == null || id <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid id");
        }
        ParamFlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofFail(Result.CODE_ERROR, "id " + id + " does not exist");
        }

        Result<ParamFlowRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }
        if (!checkIfSupported(entity.getApp(), entity.getIp(), entity.getPort())) {
            return unsupportedVersion();
        }
        entity.setId(id);
        Date date = new Date();
        entity.setGmtCreate(oldEntity.getGmtCreate());
        entity.setGmtModified(date);
        try {
            entity = repository.save(entity);
            publishRules(entity.getApp());
            return Result.ofSuccess(entity);
        } catch (Exception e) {
            logger.error("Error when updating parameter flow rules, id={}", id, e);
            return Result.ofFail(Result.CODE_ERROR, e.getMessage());
        }
    }

    /** 删除指定 id 的规则并推送应用的规则；id 不存在时直接成功返回（数据为 null）。 */
    @DeleteMapping("/rule/{id}")
    @AuthAction(PrivilegeTypeEnum.DELETE_RULE)
    public Result<Long> apiDeleteRule(@PathVariable("id") Long id) {
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id cannot be null");
        }
        ParamFlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }

        try {
            repository.delete(id);
            publishRules(oldEntity.getApp());
            return Result.ofSuccess(id);
        } catch (Exception e) {
            logger.error("Error when deleting parameter flow rules", e);
            return Result.ofFail(Result.CODE_ERROR, e.getMessage());
        }
    }

    /** 将应用在本地的全部规则推送至 Nacos。 */
    private void publishRules(String app) throws Exception {
        List<ParamFlowRuleEntity> rules = repository.findAllByApp(app);
        rulePublisher.publish(app, rules);
    }

    /** 构造客户端版本不支持热点参数限流时的错误结果。 */
    private <R> Result<R> unsupportedVersion() {
        return Result.ofFail(CODE_UNSUPPORTED_CLIENT,
                "Sentinel client not supported for parameter flow control (unsupported version or dependency absent)");
    }

}
