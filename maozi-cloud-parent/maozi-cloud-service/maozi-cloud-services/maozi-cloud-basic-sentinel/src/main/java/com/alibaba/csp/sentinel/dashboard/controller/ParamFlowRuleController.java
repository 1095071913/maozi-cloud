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
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.PrivilegeType;
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
 * Controller for hot parameter (hotspot) flow control rules.
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
@RestController
@RequestMapping(value = "/paramFlow")
public class ParamFlowRuleController {

    /** The logger of this controller. */
    private final Logger logger = LoggerFactory.getLogger(ParamFlowRuleController.class);

    /** The app management service used to validate machine-to-app relationship. */
    private final AppManagement appManagement;
    /** The repository that stores parameter flow rule entities in memory. */
    private final RuleRepository<ParamFlowRuleEntity, Long> repository;

    /** The Nacos provider that pulls parameter flow rules for a given app. */
    private final DynamicRuleProvider<List<ParamFlowRuleEntity>> ruleProvider;
    /** The Nacos publisher that pushes parameter flow rules to a given app. */
    private final DynamicRulePublisher<List<ParamFlowRuleEntity>> rulePublisher;

    public ParamFlowRuleController(AppManagement appManagement,
                                   RuleRepository<ParamFlowRuleEntity, Long> repository,
                                   @Qualifier("paramFlowRuleNacosProvider")
                                       DynamicRuleProvider<List<ParamFlowRuleEntity>> ruleProvider,
                                   @Qualifier("paramFlowRuleNacosPublisher")
                                       DynamicRulePublisher<List<ParamFlowRuleEntity>> rulePublisher) {
        this.appManagement = appManagement;
        this.repository = repository;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /**
     * Whether the machine's client version supports parameter flow control (>= 0.2.0);
     * returns true when the machine info is unavailable or an error occurs.
     */
    private boolean checkIfSupported(String app, String ip, int port) {
        try {
            return Optional.ofNullable(appManagement.getDetailApp(app))
                .flatMap(e -> e.getMachine(ip, port))
                .flatMap(m -> VersionUtils.parseVersion(m.getVersion())
                    .map(v -> v.greaterOrEqual(version020)))
                .orElse(true);
            // If error occurred or cannot retrieve machine info, return true.
        } catch (Exception ex) {
            return true;
        }
    }

    /** Fetches the app's parameter flow rules from Nacos, stamps machine info, persists them and returns them. */
    @GetMapping("/rules")
    @AuthAction(PrivilegeType.READ_RULE)
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
        } catch (Throwable throwable) {
            logger.error("Error when querying parameter flow rules", throwable);
            return Result.ofFail(Result.CODE_ERROR, throwable.getMessage());
        }
    }

    /** Adds a new rule after validation and publishes the app's rules to Nacos. */
    @PostMapping("/rule")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
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
        } catch (Throwable throwable) {
            logger.error("Error when adding new parameter flow rules", throwable);
            return Result.ofFail(Result.CODE_ERROR, throwable.getMessage());
        }
    }

    /**
     * Validates the base machine fields via the shared validator, then the param-flow-rule specific fields; returns
     * the first error or null.
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
        if (entity.getCount() < 0) {
            return Result.ofFail(Result.CODE_ERROR, "count should be valid");
        }
        if (entity.getGrade() != RuleConstant.FLOW_GRADE_QPS) {
            return Result.ofFail(Result.CODE_ERROR, "Unknown mode (blockGrade) for parameter flow control");
        }
        // Cache the param index to avoid repeated calls within the same method.
        Integer paramIdx = entity.getParamIdx();
        if (paramIdx == null || paramIdx < 0) {
            return Result.ofFail(Result.CODE_ERROR, "paramIdx should be valid");
        }
        if (entity.getDurationInSec() <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "durationInSec should be valid");
        }
        if (entity.getControlBehavior() < 0) {
            return Result.ofFail(Result.CODE_ERROR, "controlBehavior should be valid");
        }
        return null;
    }

    /** Updates the rule of the given id (gmtCreate inherited from the old entity) and publishes the app's rules. */
    @PutMapping("/rule/{id}")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
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
        } catch (Throwable throwable) {
            logger.error("Error when updating parameter flow rules, id={}", id, throwable);
            return Result.ofFail(Result.CODE_ERROR, throwable.getMessage());
        }
    }

    /** Deletes the rule of the given id and publishes the app's rules; an unknown id returns success with null data. */
    @DeleteMapping("/rule/{id}")
    @AuthAction(PrivilegeType.DELETE_RULE)
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
        } catch (Throwable throwable) {
            logger.error("Error when deleting parameter flow rules", throwable);
            return Result.ofFail(Result.CODE_ERROR, throwable.getMessage());
        }
    }

    /** Publishes all local rules of the app to Nacos. */
    private void publishRules(String app) throws Exception {
        List<ParamFlowRuleEntity> rules = repository.findAllByApp(app);
        rulePublisher.publish(app, rules);
    }

    /** Error code returned when the client version does not support parameter flow control. */
    private static final int CODE_UNSUPPORTED_CLIENT = 4041;

    /** Builds the error result returned when the client version does not support parameter flow control. */
    private <R> Result<R> unsupportedVersion() {
        return Result.ofFail(CODE_UNSUPPORTED_CLIENT,
            "Sentinel client not supported for parameter flow control (unsupported version or dependency absent)");
    }

    /** The minimum Sentinel client version (0.2.x) that supports parameter flow control. */
    private final SentinelVersion version020 = new SentinelVersion().setMinorVersion(2);
}
