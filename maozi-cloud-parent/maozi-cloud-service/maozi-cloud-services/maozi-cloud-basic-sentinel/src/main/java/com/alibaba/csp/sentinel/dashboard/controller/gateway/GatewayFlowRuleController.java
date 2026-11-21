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
package com.alibaba.csp.sentinel.dashboard.controller.gateway;


import com.alibaba.csp.sentinel.dashboard.auth.AuthAction;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService;
import com.alibaba.csp.sentinel.dashboard.controller.RuleEntityValidator;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayParamFlowItemEntity;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule.AddFlowRuleReqVo;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule.GatewayParamFlowItemVo;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule.UpdateFlowRuleReqVo;
import com.alibaba.csp.sentinel.dashboard.repository.gateway.InMemGatewayFlowRuleStore;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_MATCH_STRATEGY_CONTAINS;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_MATCH_STRATEGY_EXACT;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_MATCH_STRATEGY_REGEX;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_PARSE_STRATEGY_COOKIE;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HOST;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID;
import static com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity.INTERVAL_UNIT_DAY;
import static com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity.INTERVAL_UNIT_HOUR;
import static com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity.INTERVAL_UNIT_MINUTE;
import static com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity.INTERVAL_UNIT_SECOND;
import static com.alibaba.csp.sentinel.slots.block.RuleConstant.CONTROL_BEHAVIOR_DEFAULT;
import static com.alibaba.csp.sentinel.slots.block.RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER;
import static com.alibaba.csp.sentinel.slots.block.RuleConstant.FLOW_GRADE_QPS;
import static com.alibaba.csp.sentinel.slots.block.RuleConstant.FLOW_GRADE_THREAD;

/**
 * Controller for managing gateway flow rules.
 *
 * @author cdfive
 * @since 1.7.0
 */
@RestController
@RequestMapping(value = "/gateway/flow")
public class GatewayFlowRuleController {

    /** The logger of this controller. */
    private final Logger logger = LoggerFactory.getLogger(GatewayFlowRuleController.class);

    /** The in-memory store that keeps gateway flow rules. */
    private final InMemGatewayFlowRuleStore repository;

    /** The Nacos provider that pulls gateway flow rules for a given app. */
    private final DynamicRuleProvider<List<GatewayFlowRuleEntity>> ruleProvider;
    /** The Nacos publisher that pushes gateway flow rules to a given app. */
    private final DynamicRulePublisher<List<GatewayFlowRuleEntity>> rulePublisher;

    public GatewayFlowRuleController(InMemGatewayFlowRuleStore repository,
                                     @Qualifier("gatewayFlowRuleNacosProvider")
                                         DynamicRuleProvider<List<GatewayFlowRuleEntity>> ruleProvider,
                                     @Qualifier("gatewayFlowRuleNacosPublisher")
                                         DynamicRulePublisher<List<GatewayFlowRuleEntity>> rulePublisher) {
        this.repository = repository;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /** Fetches the app's gateway flow rules from Nacos, stamps machine info, persists them and returns them. */
    @GetMapping("/list.json")
    @AuthAction(AuthService.PrivilegeType.READ_RULE)
    public Result<List<GatewayFlowRuleEntity>> queryFlowRules(String app, String ip, Integer port) {

        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(ip)) {
            return Result.ofFail(-1, "ip can't be null or empty");
        }
        if (port == null) {
            return Result.ofFail(-1, "port can't be null");
        }

        try {
            List<GatewayFlowRuleEntity> rules = ruleProvider.getRules(app);
            for (GatewayFlowRuleEntity entity : rules) {
                entity.setApp(app);
                entity.setIp(ip);
                entity.setPort(port);
            }
            rules = repository.saveAll(rules);
            return Result.ofSuccess(rules);
        } catch (Throwable throwable) {
            logger.error("query gateway flow rules error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }
    }

    /** Adds a new gateway flow rule after full field validation, then publishes the app's rules to Nacos. */
    @PostMapping("/new.json")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<GatewayFlowRuleEntity> addFlowRule(@RequestBody AddFlowRuleReqVo reqVo) {

        GatewayFlowRuleEntity entity = new GatewayFlowRuleEntity();
        entity.setApp(reqVo.getApp());
        entity.setIp(reqVo.getIp());
        entity.setPort(reqVo.getPort());
        Result<GatewayFlowRuleEntity> baseResult = RuleEntityValidator.checkBaseFields(entity);
        if (baseResult != null) {
            return baseResult;
        }
        entity.setApp(entity.getApp().trim());
        entity.setIp(entity.getIp().trim());

        // Resource type: Route ID or API group name.
        Integer resourceMode = reqVo.getResourceMode();
        if (resourceMode == null) {
            return Result.ofFail(-1, "resourceMode can't be null");
        }
        if (!Arrays.asList(RESOURCE_MODE_ROUTE_ID, RESOURCE_MODE_CUSTOM_API_NAME).contains(resourceMode)) {
            return Result.ofFail(-1, "invalid resourceMode: " + resourceMode);
        }
        entity.setResourceMode(resourceMode);

        // API name.
        String resource = reqVo.getResource();
        if (StringUtil.isBlank(resource)) {
            return Result.ofFail(-1, "resource can't be null or empty");
        }
        entity.setResource(resource.trim());

        Result<GatewayFlowRuleEntity> checkResult = checkAndFillParamItem(reqVo.getParamItem(), entity);
        if (checkResult != null) {
            return checkResult;
        }
        checkResult = checkAndFillRuleFields(reqVo.getGrade(), reqVo.getCount(), reqVo.getInterval(),
            reqVo.getIntervalUnit(), reqVo.getControlBehavior(), reqVo.getBurst(), reqVo.getMaxQueueingTimeoutMs(), 
                entity);
        if (checkResult != null) {
            return checkResult;
        }

        entity.setGmtCreateAndModified(new Date());
        return saveAndPublish(entity, "add");
    }

    /**
     * Updates the rule of the given id (param item replaced, or cleared when absent) after validation, then publishes
     * the app's rules.
     */
    @PostMapping("/save.json")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<GatewayFlowRuleEntity> updateFlowRule(@RequestBody UpdateFlowRuleReqVo reqVo) {

        if (StringUtil.isBlank(reqVo.getApp())) {
            return Result.ofFail(-1, "app can't be null or empty");
        }

        Long id = reqVo.getId();
        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }

        GatewayFlowRuleEntity entity = repository.findById(id);
        if (entity == null) {
            return Result.ofFail(-1, "gateway flow rule does not exist, id=" + id);
        }

        Result<GatewayFlowRuleEntity> checkResult = checkAndFillParamItem(reqVo.getParamItem(), entity);
        if (checkResult != null) {
            return checkResult;
        }
        checkResult = checkAndFillRuleFields(reqVo.getGrade(), reqVo.getCount(), reqVo.getInterval(),
            reqVo.getIntervalUnit(), reqVo.getControlBehavior(), reqVo.getBurst(), reqVo.getMaxQueueingTimeoutMs(), 
                entity);
        if (checkResult != null) {
            return checkResult;
        }

        entity.setGmtModified(new Date());
        return saveAndPublish(entity, "update");
    }


    /** Deletes the rule of the given id and publishes the app's rules; an unknown id returns success with null data. */
    @PostMapping("/delete.json")
    @AuthAction(AuthService.PrivilegeType.DELETE_RULE)
    public Result<Long> deleteFlowRule(Long id) {

        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }

        GatewayFlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }

        try {
            repository.delete(id);
        } catch (Throwable throwable) {
            logger.error("delete gateway flow rule error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }

        if (!publishRules(oldEntity.getApp())) {
            logger.warn("publish gateway flow rules fail after delete");
        }

        return Result.ofSuccess(id);
    }

    /**
     * Validates the request parameter item (parse strategy, field name for the header/URL
     * parameter/cookie strategies, and pattern with its match strategy) and fills it on the
     * entity, or clears the entity's item when the request carries none; returns the first
     * error result or null when valid.
     */
    private <R> Result<R> checkAndFillParamItem(GatewayParamFlowItemVo paramItem, GatewayFlowRuleEntity entity) {
        if (paramItem == null) {
            entity.setParamItem(null);
            return null;
        }

        GatewayParamFlowItemEntity itemEntity = new GatewayParamFlowItemEntity();
        entity.setParamItem(itemEntity);

        // Parse strategy: 0-ClientIP, 1-Remote Host, 2-Header, 3-URL parameter, 4-Cookie.
        Integer parseStrategy = paramItem.getParseStrategy();
        if (!Arrays.asList(PARAM_PARSE_STRATEGY_CLIENT_IP, PARAM_PARSE_STRATEGY_HOST, PARAM_PARSE_STRATEGY_HEADER
                , PARAM_PARSE_STRATEGY_URL_PARAM, PARAM_PARSE_STRATEGY_COOKIE).contains(parseStrategy)) {
            return Result.ofFail(-1, "invalid parseStrategy: " + parseStrategy);
        }
        itemEntity.setParseStrategy(parseStrategy);

        // Field name is required for parse strategies 2-Header, 3-URL parameter and 4-Cookie.
        if (Arrays.asList(PARAM_PARSE_STRATEGY_HEADER, PARAM_PARSE_STRATEGY_URL_PARAM, 
            PARAM_PARSE_STRATEGY_COOKIE).contains(parseStrategy)) {
            // Field name.
            String fieldName = paramItem.getFieldName();
            if (StringUtil.isBlank(fieldName)) {
                return Result.ofFail(-1, "fieldName can't be null or empty");
            }
            itemEntity.setFieldName(fieldName);
        }

        String pattern = paramItem.getPattern();
        // Validate the match strategy only when the pattern is not empty.
        if (StringUtil.isNotEmpty(pattern)) {
            itemEntity.setPattern(pattern);
            Integer matchStrategy = paramItem.getMatchStrategy();
            if (!Arrays.asList(PARAM_MATCH_STRATEGY_EXACT, PARAM_MATCH_STRATEGY_CONTAINS, 
                PARAM_MATCH_STRATEGY_REGEX).contains(matchStrategy)) {
                return Result.ofFail(-1, "invalid matchStrategy: " + matchStrategy);
            }
            itemEntity.setMatchStrategy(matchStrategy);
        }
        return null;
    }

    /**
     * Validates the rule fields shared by add and update (grade, count, interval with its
     * unit, and controlBehavior with the burst / maxQueueingTimeoutMs it requires) and
     * fills them on the entity; returns the first error result or null when valid.
     */
    private <R> Result<R> checkAndFillRuleFields(Integer grade, Double count, Long interval, Integer intervalUnit,
                                                 Integer controlBehavior, Integer burst, Integer maxQueueingTimeoutMs,
                                                 GatewayFlowRuleEntity entity) {
        // Threshold type: 0-thread count, 1-QPS.
        if (grade == null) {
            return Result.ofFail(-1, "grade can't be null");
        }
        if (!Arrays.asList(FLOW_GRADE_THREAD, FLOW_GRADE_QPS).contains(grade)) {
            return Result.ofFail(-1, "invalid grade: " + grade);
        }
        entity.setGrade(grade);

        // QPS threshold.
        if (count == null) {
            return Result.ofFail(-1, "count can't be null");
        }
        if (count < 0) {
            return Result.ofFail(-1, "count should be at lease zero");
        }
        entity.setCount(count);

        // Interval.
        if (interval == null) {
            return Result.ofFail(-1, "interval can't be null");
        }
        if (interval <= 0) {
            return Result.ofFail(-1, "interval should be greater than zero");
        }
        entity.setInterval(interval);

        // Interval unit.
        if (intervalUnit == null) {
            return Result.ofFail(-1, "intervalUnit can't be null");
        }
        if (!Arrays.asList(INTERVAL_UNIT_SECOND, INTERVAL_UNIT_MINUTE, INTERVAL_UNIT_HOUR, 
            INTERVAL_UNIT_DAY).contains(intervalUnit)) {
            return Result.ofFail(-1, "Invalid intervalUnit: " + intervalUnit);
        }
        entity.setIntervalUnit(intervalUnit);

        // Control behavior: 0-fast fail, 2-uniform rate limiter.
        if (controlBehavior == null) {
            return Result.ofFail(-1, "controlBehavior can't be null");
        }
        if (!Arrays.asList(CONTROL_BEHAVIOR_DEFAULT, CONTROL_BEHAVIOR_RATE_LIMITER).contains(controlBehavior)) {
            return Result.ofFail(-1, "invalid controlBehavior: " + controlBehavior);
        }
        entity.setControlBehavior(controlBehavior);

        if (CONTROL_BEHAVIOR_DEFAULT == controlBehavior) {
            // Burst size is required for control behavior 0 (fast fail).
            if (burst == null) {
                return Result.ofFail(-1, "burst can't be null");
            }
            if (burst < 0) {
                return Result.ofFail(-1, "invalid burst: " + burst);
            }
            entity.setBurst(burst);
        } else if (CONTROL_BEHAVIOR_RATE_LIMITER == controlBehavior) {
            // Timeout is required for control behavior 2 (uniform rate limiter).
            if (maxQueueingTimeoutMs == null) {
                return Result.ofFail(-1, "maxQueueingTimeoutMs can't be null");
            }
            if (maxQueueingTimeoutMs < 0) {
                return Result.ofFail(-1, "invalid maxQueueingTimeoutMs: " + maxQueueingTimeoutMs);
            }
            entity.setMaxQueueingTimeoutMs(maxQueueingTimeoutMs);
        }
        return null;
    }

    /**
     * Saves the entity and publishes the app's rules (logging the given action on failure); returns the saved entity
     * or a failure result.
     */
    private Result<GatewayFlowRuleEntity> saveAndPublish(GatewayFlowRuleEntity entity, String action) {
        try {
            entity = repository.save(entity);
        } catch (Throwable throwable) {
            logger.error(action + " gateway flow rule error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }

        if (!publishRules(entity.getApp())) {
            logger.warn("publish gateway flow rules fail after {}", action);
        }

        return Result.ofSuccess(entity);
    }

    /** Publishes all local rules of the app to Nacos; returns false on error. */
    private boolean publishRules(String app) {
        try {
            List<GatewayFlowRuleEntity> rules = repository.findAllByApp(app);
            rulePublisher.publish(app, rules);
            return true;
        } catch (Exception e) {
            logger.error("Error when publishing gateway flow rules to Nacos, app={}", app, e);
            return false;
        }
    }
}
