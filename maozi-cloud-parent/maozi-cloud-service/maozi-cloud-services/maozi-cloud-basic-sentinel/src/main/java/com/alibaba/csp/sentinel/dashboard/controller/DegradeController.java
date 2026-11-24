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
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.PrivilegeTypeEnum;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.RuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 降级规则管理控制器，通过动态规则提供者/发布者从 Nacos 拉取与推送规则。
 *
 * @author Carpenter Lee
 * @author Eric Zhao
 * @date 2026/11/22
 */
@RestController
@RequestMapping("/degrade")
public class DegradeController {

    /** 日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(DegradeController.class);

    /** 存储降级规则实体的内存仓储。 */
    private final RuleRepository<DegradeRuleEntity, Long> repository;
    /** 用于校验机器与应用归属关系的应用管理服务。 */
    private final AppManagement appManagement;
    /** 从 Nacos 拉取指定应用降级规则的提供者。 */
    private final DynamicRuleProvider<List<DegradeRuleEntity>> ruleProvider;
    /** 向指定应用推送降级规则的发布者。 */
    private final DynamicRulePublisher<List<DegradeRuleEntity>> rulePublisher;

    /** 注入规则仓储、应用管理、规则提供者与发布者。 */
    public DegradeController(RuleRepository<DegradeRuleEntity, Long> repository, AppManagement appManagement,
            @Qualifier("degradeRuleNacosProvider") DynamicRuleProvider<List<DegradeRuleEntity>> ruleProvider,
            @Qualifier("degradeRuleNacosPublisher") DynamicRulePublisher<List<DegradeRuleEntity>> rulePublisher) {
        this.repository = repository;
        this.appManagement = appManagement;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /**
     * 从 Nacos 拉取应用的降级规则，为每条规则补充指定机器信息后持久化到仓储并返回。
     */
    @GetMapping("/rules.json")
    @AuthAction(PrivilegeTypeEnum.READ_RULE)
    public Result<List<DegradeRuleEntity>> apiQueryMachineRules(String app, String ip, Integer port) {
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
        try {
            List<DegradeRuleEntity> rules = ruleProvider.getRules(app);
            for (DegradeRuleEntity entity : rules) {
                entity.setApp(app);
                entity.setIp(ip);
                entity.setPort(port);
            }
            rules = repository.saveAll(rules);
            return Result.ofSuccess(rules);
        } catch (Exception e) {
            logger.error("Error when querying degrade rules", e);
            return Result.ofThrowable(Result.CODE_ERROR, e);
        }
    }

    /** 校验通过后新增降级规则，并将应用的规则推送至 Nacos。 */
    @PostMapping("/rule")
    @AuthAction(PrivilegeTypeEnum.WRITE_RULE)
    public Result<DegradeRuleEntity> apiAddRule(@RequestBody DegradeRuleEntity entity) {
        Result<DegradeRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }
        entity.setGmtCreateAndModified(new Date());
        try {
            entity = repository.save(entity);
        } catch (Exception e) {
            logger.error("Failed to add new degrade rule, app={}, ip={}", entity.getApp(), entity.getIp(), e);
            return Result.ofThrowable(Result.CODE_ERROR, e);
        }
        // 缓存应用名，规则推送与失败日志共用，避免同一方法内重复调用
        String app = entity.getApp();
        if (!publishRules(app)) {
            logger.warn("Publish degrade rules failed, app={}", app);
        }
        return Result.ofSuccess(entity);
    }

    /** 更新指定 id 的规则（app/ip/port/id/gmtCreate 沿用旧实体），并推送应用的规则。 */
    @PutMapping("/rule/{id}")
    @AuthAction(PrivilegeTypeEnum.WRITE_RULE)
    public Result<DegradeRuleEntity> apiUpdateRule(@PathVariable("id") Long id,
            @RequestBody DegradeRuleEntity entity) {
        if (id == null || id <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null or negative");
        }
        DegradeRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofFail(Result.CODE_ERROR, "Degrade rule does not exist, id=" + id);
        }
        entity.setApp(oldEntity.getApp());
        entity.setIp(oldEntity.getIp());
        entity.setPort(oldEntity.getPort());
        entity.setId(oldEntity.getId());
        Result<DegradeRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }

        entity.setGmtCreate(oldEntity.getGmtCreate());
        entity.setGmtModified(new Date());
        try {
            entity = repository.save(entity);
        } catch (Exception e) {
            logger.error("Failed to save degrade rule, id={}, rule={}", id, entity, e);
            return Result.ofThrowable(Result.CODE_ERROR, e);
        }
        // 缓存应用名，规则推送与失败日志共用，避免同一方法内重复调用
        String app = entity.getApp();
        if (!publishRules(app)) {
            logger.warn("Publish degrade rules failed, app={}", app);
        }
        return Result.ofSuccess(entity);
    }

    /** 删除指定 id 的规则并推送应用的规则；id 不存在时直接成功返回（数据为 null）。 */
    @DeleteMapping("/rule/{id}")
    @AuthAction(PrivilegeTypeEnum.DELETE_RULE)
    public Result<Long> delete(@PathVariable("id") Long id) {
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }

        DegradeRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }

        try {
            repository.delete(id);
        } catch (Exception e) {
            logger.error("Failed to delete degrade rule, id={}", id, e);
            return Result.ofThrowable(Result.CODE_ERROR, e);
        }
        // 缓存应用名，规则推送与失败日志共用，避免同一方法内重复调用
        String app = oldEntity.getApp();
        if (!publishRules(app)) {
            logger.warn("Publish degrade rules failed, app={}", app);
        }
        return Result.ofSuccess(id);
    }

    /** 将应用在本地的全部规则推送至 Nacos；出错时返回 false。 */
    private boolean publishRules(String app) {
        try {
            List<DegradeRuleEntity> rules = repository.findAllByApp(app);
            rulePublisher.publish(app, rules);
            return true;
        } catch (Exception e) {
            logger.error("Error when publishing degrade rules to Nacos, app={}", app, e);
            return false;
        }
    }

    /**
     * 先通过公共校验器校验机器字段，再校验降级规则特有字段；返回首个错误结果，校验通过返回 null。
     */
    private <R> Result<R> checkEntityInternal(DegradeRuleEntity entity) {
        Result<R> baseResult = RuleEntityValidator.checkBaseFields(entity, appManagement);
        if (baseResult != null) {
            return baseResult;
        }
        if (StringUtil.isBlank(entity.getLimitApp())) {
            return Result.ofFail(Result.CODE_ERROR, "limitApp can't be null or empty");
        }
        if (StringUtil.isBlank(entity.getResource())) {
            return Result.ofFail(Result.CODE_ERROR, "resource can't be null or empty");
        }
        Double threshold = entity.getCount();
        if (threshold == null || threshold < 0) {
            return Result.ofFail(Result.CODE_ERROR, "invalid threshold: " + threshold);
        }
        Integer recoveryTimeoutSec = entity.getTimeWindow();
        if (recoveryTimeoutSec == null || recoveryTimeoutSec <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "recoveryTimeout should be positive");
        }
        Integer strategy = entity.getGrade();
        if (strategy == null) {
            return Result.ofFail(Result.CODE_ERROR, "circuit breaker strategy cannot be null");
        }
        if (strategy < CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType()
                || strategy > RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid circuit breaker strategy: " + strategy);
        }
        Integer minRequestAmount = entity.getMinRequestAmount();
        if (minRequestAmount == null || minRequestAmount <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid minRequestAmount");
        }
        Integer statIntervalMs = entity.getStatIntervalMs();
        if (statIntervalMs == null || statIntervalMs <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid statInterval");
        }
        if (strategy == RuleConstant.DEGRADE_GRADE_RT) {
            Double slowRatio = entity.getSlowRatioThreshold();
            if (slowRatio == null) {
                return Result.ofFail(Result.CODE_ERROR,
                        "SlowRatioThreshold is required for slow request ratio strategy");
            }
            if (slowRatio < 0 || slowRatio > 1) {
                return Result.ofFail(Result.CODE_ERROR, "SlowRatioThreshold should be in range: [0.0, 1.0]");
            }
        } else if (strategy == RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO && threshold > 1) {
            return Result.ofFail(Result.CODE_ERROR, "Ratio threshold should be in range: [0.0, 1.0]");
        }
        return null;
    }
}
