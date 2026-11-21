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
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.RuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
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

/**
 * 授权（黑白名单）规则管理控制器，通过动态规则提供者/发布者从 Nacos
 * 拉取与推送规则。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
@RestController
@RequestMapping(value = "/authority")
public class AuthorityRuleController {

    /** 日志记录器。 */
    private final Logger logger = LoggerFactory.getLogger(AuthorityRuleController.class);

    /** 授权规则实体的本地存储。 */
    private final RuleRepository<AuthorityRuleEntity, Long> repository;
    /** 应用与机器注册表，用于机器有效性校验。 */
    private final AppManagement appManagement;

    /** 基于 Nacos 的授权规则动态提供者。 */
    private final DynamicRuleProvider<List<AuthorityRuleEntity>> ruleProvider;
    /** 基于 Nacos 的授权规则动态发布者。 */
    private final DynamicRulePublisher<List<AuthorityRuleEntity>> rulePublisher;

    /** 注入规则仓储、应用管理、规则提供者与发布者。 */
    public AuthorityRuleController(RuleRepository<AuthorityRuleEntity, Long> repository,
                                   AppManagement appManagement,
                                   @Qualifier("authorityRuleNacosProvider")
                                       DynamicRuleProvider<List<AuthorityRuleEntity>> ruleProvider,
                                   @Qualifier("authorityRuleNacosPublisher")
                                       DynamicRulePublisher<List<AuthorityRuleEntity>> rulePublisher) {
        this.repository = repository;
        this.appManagement = appManagement;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /**
     * 从动态提供者查询应用的授权规则，为每个实体补充指定机器信息后
     * 持久化到本地仓储。
     */
    @GetMapping("/rules")
    @AuthAction(PrivilegeType.READ_RULE)
    public Result<List<AuthorityRuleEntity>> apiQueryAllRulesForMachine(@RequestParam String app,
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
        try {
            List<AuthorityRuleEntity> rules = ruleProvider.getRules(app);
            for (AuthorityRuleEntity entity : rules) {
                entity.setApp(app);
                entity.setIp(ip);
                entity.setPort(port);
            }
            rules = repository.saveAll(rules);
            return Result.ofSuccess(rules);
        } catch (Throwable throwable) {
            logger.error("Error when querying authority rules", throwable);
            return Result.ofFail(Result.CODE_ERROR, throwable.getMessage());
        }
    }

    /**
     * 先通过公共校验器校验基础机器字段，再校验授权规则特有字段；
     * 返回首个错误结果，校验通过返回 null。
     */
    private <R> Result<R> checkEntityInternal(AuthorityRuleEntity entity) {
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
        if (StringUtil.isBlank(entity.getLimitApp())) {
            return Result.ofFail(Result.CODE_ERROR, "limitApp should be valid");
        }
        Integer strategy = entity.getStrategy();
        if (strategy != RuleConstant.AUTHORITY_WHITE
            && strategy != RuleConstant.AUTHORITY_BLACK) {
            return Result.ofFail(Result.CODE_ERROR, "Unknown strategy (must be blacklist or whitelist)");
        }
        return null;
    }

    /** 校验通过后新增规则，并将应用的规则推送至 Nacos。 */
    @PostMapping("/rule")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<AuthorityRuleEntity> apiAddAuthorityRule(@RequestBody AuthorityRuleEntity entity) {
        Result<AuthorityRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }
        entity.setId(null);
        entity.setGmtCreateAndModified(new Date());
        try {
            entity = repository.save(entity);
        } catch (Throwable throwable) {
            logger.error("Failed to add authority rule", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
        if (!publishRules(entity.getApp())) {
            logger.info("Publish authority rules failed after rule add");
        }
        return Result.ofSuccess(entity);
    }

    /** 更新指定 id 的规则（清空 gmtCreate），并将应用的规则推送至 Nacos。 */
    @PutMapping("/rule/{id}")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<AuthorityRuleEntity> apiUpdateParamFlowRule(@PathVariable("id") Long id,
                                                              @RequestBody AuthorityRuleEntity entity) {
        if (id == null || id <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid id");
        }
        Result<AuthorityRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }
        entity.setId(id);
        Date date = new Date();
        entity.setGmtCreate(null);
        entity.setGmtModified(date);
        try {
            entity = repository.save(entity);
            if (entity == null) {
                return Result.ofFail(Result.CODE_ERROR, "Failed to save authority rule");
            }
        } catch (Throwable throwable) {
            logger.error("Failed to save authority rule", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
        if (!publishRules(entity.getApp())) {
            logger.info("Publish authority rules failed after rule update");
        }
        return Result.ofSuccess(entity);
    }

    /** 删除指定 id 的规则并推送应用的规则；id 不存在时直接成功返回（数据为 null）。 */
    @DeleteMapping("/rule/{id}")
    @AuthAction(PrivilegeType.DELETE_RULE)
    public Result<Long> apiDeleteRule(@PathVariable("id") Long id) {
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id cannot be null");
        }
        AuthorityRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }
        try {
            repository.delete(id);
        } catch (Exception e) {
            return Result.ofFail(Result.CODE_ERROR, e.getMessage());
        }
        if (!publishRules(oldEntity.getApp())) {
            logger.error("Publish authority rules failed after rule delete");
        }
        return Result.ofSuccess(id);
    }

    /** 将应用在本地的全部规则推送至 Nacos；出错时返回 false。 */
    private boolean publishRules(String app) {
        try {
            List<AuthorityRuleEntity> rules = repository.findAllByApp(app);
            rulePublisher.publish(app, rules);
            return true;
        } catch (Exception e) {
            logger.error("Error when publishing authority rules to Nacos, app={}", app, e);
            return false;
        }
    }
}
