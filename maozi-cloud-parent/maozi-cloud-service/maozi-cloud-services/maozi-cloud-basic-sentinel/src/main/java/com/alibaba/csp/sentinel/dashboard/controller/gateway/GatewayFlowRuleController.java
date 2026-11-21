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
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule.AddFlowRuleReqVO;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule.GatewayParamFlowItemVO;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule.UpdateFlowRuleReqVO;
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
 * 网关流控规则管理控制器。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
@RestController
@RequestMapping(value = "/gateway/flow")
public class GatewayFlowRuleController {

    /** 日志记录器。 */
    private final Logger logger = LoggerFactory.getLogger(GatewayFlowRuleController.class);

    /** 保存网关流控规则的内存仓储。 */
    private final InMemGatewayFlowRuleStore repository;

    /** 从 Nacos 拉取指定应用网关流控规则的提供者。 */
    private final DynamicRuleProvider<List<GatewayFlowRuleEntity>> ruleProvider;
    /** 向指定应用推送网关流控规则的发布者。 */
    private final DynamicRulePublisher<List<GatewayFlowRuleEntity>> rulePublisher;

    /** 注入网关流控规则仓储、规则提供者与发布者。 */
    public GatewayFlowRuleController(InMemGatewayFlowRuleStore repository,
                                     @Qualifier("gatewayFlowRuleNacosProvider")
                                         DynamicRuleProvider<List<GatewayFlowRuleEntity>> ruleProvider,
                                     @Qualifier("gatewayFlowRuleNacosPublisher")
                                         DynamicRulePublisher<List<GatewayFlowRuleEntity>> rulePublisher) {
        this.repository = repository;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /** 从 Nacos 拉取应用的网关流控规则，为每条补充机器信息后持久化并返回。 */
    @GetMapping("/list.json")
    @AuthAction(AuthService.PrivilegeType.READ_RULE)
    public Result<List<GatewayFlowRuleEntity>> queryFlowRules(String app, String ip, Integer port) {

        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(ip)) {
            return Result.ofFail(Result.CODE_ERROR, "ip can't be null or empty");
        }
        if (port == null) {
            return Result.ofFail(Result.CODE_ERROR, "port can't be null");
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
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
    }

    /** 全字段校验通过后新增网关流控规则，并将应用的规则推送至 Nacos。 */
    @PostMapping("/new.json")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<GatewayFlowRuleEntity> addFlowRule(@RequestBody AddFlowRuleReqVO reqVo) {

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

        // 资源类型：Route ID 或 API 分组名
        Integer resourceMode = reqVo.getResourceMode();
        if (resourceMode == null) {
            return Result.ofFail(Result.CODE_ERROR, "resourceMode can't be null");
        }
        if (!Arrays.asList(RESOURCE_MODE_ROUTE_ID, RESOURCE_MODE_CUSTOM_API_NAME).contains(resourceMode)) {
            return Result.ofFail(Result.CODE_ERROR, "invalid resourceMode: " + resourceMode);
        }
        entity.setResourceMode(resourceMode);

        // API 名称
        String resource = reqVo.getResource();
        if (StringUtil.isBlank(resource)) {
            return Result.ofFail(Result.CODE_ERROR, "resource can't be null or empty");
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
     * 更新指定 id 的规则（参数条目被替换，请求未携带时清空），校验通过后推送应用的规则。
     */
    @PostMapping("/save.json")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<GatewayFlowRuleEntity> updateFlowRule(@RequestBody UpdateFlowRuleReqVO reqVo) {

        if (StringUtil.isBlank(reqVo.getApp())) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }

        Long id = reqVo.getId();
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }

        GatewayFlowRuleEntity entity = repository.findById(id);
        if (entity == null) {
            return Result.ofFail(Result.CODE_ERROR, "gateway flow rule does not exist, id=" + id);
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


    /** 删除指定 id 的规则并推送应用的规则；id 不存在时直接成功返回（数据为 null）。 */
    @PostMapping("/delete.json")
    @AuthAction(AuthService.PrivilegeType.DELETE_RULE)
    public Result<Long> deleteFlowRule(Long id) {

        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }

        GatewayFlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }

        try {
            repository.delete(id);
        } catch (Throwable throwable) {
            logger.error("delete gateway flow rule error:", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }

        if (!publishRules(oldEntity.getApp())) {
            logger.warn("publish gateway flow rules fail after delete");
        }

        return Result.ofSuccess(id);
    }

    /**
     * 校验请求中的参数条目（解析策略、Header/URL 参数/Cookie 策略所需的字段名、
     * 匹配模式及其匹配策略）并填充到实体；请求未携带时清空实体的参数条目；
     * 返回首个错误结果，校验通过返回 null。
     */
    private <R> Result<R> checkAndFillParamItem(GatewayParamFlowItemVO paramItem, GatewayFlowRuleEntity entity) {
        if (paramItem == null) {
            entity.setParamItem(null);
            return null;
        }

        GatewayParamFlowItemEntity itemEntity = new GatewayParamFlowItemEntity();
        entity.setParamItem(itemEntity);

        // 解析策略：0-ClientIP、1-Remote Host、2-Header、3-URL 参数、4-Cookie
        Integer parseStrategy = paramItem.getParseStrategy();
        if (!Arrays.asList(PARAM_PARSE_STRATEGY_CLIENT_IP, PARAM_PARSE_STRATEGY_HOST, PARAM_PARSE_STRATEGY_HEADER
                , PARAM_PARSE_STRATEGY_URL_PARAM, PARAM_PARSE_STRATEGY_COOKIE).contains(parseStrategy)) {
            return Result.ofFail(Result.CODE_ERROR, "invalid parseStrategy: " + parseStrategy);
        }
        itemEntity.setParseStrategy(parseStrategy);

        // 解析策略 2-Header、3-URL 参数、4-Cookie 必须提供字段名
        if (Arrays.asList(PARAM_PARSE_STRATEGY_HEADER, PARAM_PARSE_STRATEGY_URL_PARAM, 
            PARAM_PARSE_STRATEGY_COOKIE).contains(parseStrategy)) {
            // 字段名
            String fieldName = paramItem.getFieldName();
            if (StringUtil.isBlank(fieldName)) {
                return Result.ofFail(Result.CODE_ERROR, "fieldName can't be null or empty");
            }
            itemEntity.setFieldName(fieldName);
        }

        String pattern = paramItem.getPattern();
        // 匹配模式非空时才校验匹配策略
        if (StringUtil.isNotEmpty(pattern)) {
            itemEntity.setPattern(pattern);
            Integer matchStrategy = paramItem.getMatchStrategy();
            if (!Arrays.asList(PARAM_MATCH_STRATEGY_EXACT, PARAM_MATCH_STRATEGY_CONTAINS, 
                PARAM_MATCH_STRATEGY_REGEX).contains(matchStrategy)) {
                return Result.ofFail(Result.CODE_ERROR, "invalid matchStrategy: " + matchStrategy);
            }
            itemEntity.setMatchStrategy(matchStrategy);
        }
        return null;
    }

    /**
     * 校验新增与更新共有的规则字段（grade、count、interval 及其单位、controlBehavior
     * 及其要求的 burst / maxQueueingTimeoutMs）并填充到实体；
     * 返回首个错误结果，校验通过返回 null。
     */
    private <R> Result<R> checkAndFillRuleFields(Integer grade, Double count, Long interval, Integer intervalUnit,
                                                 Integer controlBehavior, Integer burst, Integer maxQueueingTimeoutMs,
                                                 GatewayFlowRuleEntity entity) {
        // 阈值类型：0-线程数、1-QPS
        if (grade == null) {
            return Result.ofFail(Result.CODE_ERROR, "grade can't be null");
        }
        if (!Arrays.asList(FLOW_GRADE_THREAD, FLOW_GRADE_QPS).contains(grade)) {
            return Result.ofFail(Result.CODE_ERROR, "invalid grade: " + grade);
        }
        entity.setGrade(grade);

        // QPS 阈值
        if (count == null) {
            return Result.ofFail(Result.CODE_ERROR, "count can't be null");
        }
        if (count < 0) {
            return Result.ofFail(Result.CODE_ERROR, "count should be at lease zero");
        }
        entity.setCount(count);

        // 统计窗口时长
        if (interval == null) {
            return Result.ofFail(Result.CODE_ERROR, "interval can't be null");
        }
        if (interval <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "interval should be greater than zero");
        }
        entity.setInterval(interval);

        // 统计窗口时长单位
        if (intervalUnit == null) {
            return Result.ofFail(Result.CODE_ERROR, "intervalUnit can't be null");
        }
        if (!Arrays.asList(INTERVAL_UNIT_SECOND, INTERVAL_UNIT_MINUTE, INTERVAL_UNIT_HOUR, 
            INTERVAL_UNIT_DAY).contains(intervalUnit)) {
            return Result.ofFail(Result.CODE_ERROR, "Invalid intervalUnit: " + intervalUnit);
        }
        entity.setIntervalUnit(intervalUnit);

        // 流控效果：0-快速失败、2-匀速排队
        if (controlBehavior == null) {
            return Result.ofFail(Result.CODE_ERROR, "controlBehavior can't be null");
        }
        if (!Arrays.asList(CONTROL_BEHAVIOR_DEFAULT, CONTROL_BEHAVIOR_RATE_LIMITER).contains(controlBehavior)) {
            return Result.ofFail(Result.CODE_ERROR, "invalid controlBehavior: " + controlBehavior);
        }
        entity.setControlBehavior(controlBehavior);

        if (CONTROL_BEHAVIOR_DEFAULT == controlBehavior) {
            // 流控效果 0（快速失败）必须提供突发增量
            if (burst == null) {
                return Result.ofFail(Result.CODE_ERROR, "burst can't be null");
            }
            if (burst < 0) {
                return Result.ofFail(Result.CODE_ERROR, "invalid burst: " + burst);
            }
            entity.setBurst(burst);
        } else if (CONTROL_BEHAVIOR_RATE_LIMITER == controlBehavior) {
            // 流控效果 2（匀速排队）必须提供排队超时时间
            if (maxQueueingTimeoutMs == null) {
                return Result.ofFail(Result.CODE_ERROR, "maxQueueingTimeoutMs can't be null");
            }
            if (maxQueueingTimeoutMs < 0) {
                return Result.ofFail(Result.CODE_ERROR, "invalid maxQueueingTimeoutMs: " + maxQueueingTimeoutMs);
            }
            entity.setMaxQueueingTimeoutMs(maxQueueingTimeoutMs);
        }
        return null;
    }

    /**
     * 保存实体并推送应用的规则（失败时记录指定动作的日志）；返回保存后的实体或失败结果。
     */
    private Result<GatewayFlowRuleEntity> saveAndPublish(GatewayFlowRuleEntity entity, String action) {
        try {
            entity = repository.save(entity);
        } catch (Throwable throwable) {
            logger.error("{} gateway flow rule error:", action, throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }

        if (!publishRules(entity.getApp())) {
            logger.warn("publish gateway flow rules fail after {}", action);
        }

        return Result.ofSuccess(entity);
    }

    /** 将应用在本地的全部规则推送至 Nacos；出错时返回 false。 */
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
