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

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.util.StringUtil;

/**
 * 规则实体的公共校验工具类，从各规则控制器的 {@code checkEntityInternal}
 * 方法中抽取，使公共校验只编写一次。
 * @author maozi
 * @date 2026/11/22
 */
public final class RuleEntityValidator {

    /** 工具类禁止实例化。 */
    private RuleEntityValidator() {
    }

    /**
     * 校验全部规则实体共有的基础机器字段（app、ip、port）；
     * 返回首个错误结果，校验通过返回 null。
     */
    public static <R> Result<R> checkBaseFields(RuleEntity entity) {
        if (StringUtil.isBlank(entity.getApp())) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }
        if (StringUtil.isBlank(entity.getIp())) {
            return Result.ofFail(Result.CODE_ERROR, "ip can't be null or empty");
        }
        Integer port = entity.getPort();
        if (port == null || port <= 0) {
            return Result.ofFail(Result.CODE_ERROR, "port can't be null");
        }
        return null;
    }

    /**
     * 在 {@link #checkBaseFields(RuleEntity)} 基础上追加校验 ip 是否属于指定应用；
     * 返回首个错误结果，校验通过返回 null。
     */
    public static <R> Result<R> checkBaseFields(RuleEntity entity, AppManagement appManagement) {
        Result<R> result = checkBaseFields(entity);
        if (result != null) {
            return result;
        }
        if (!appManagement.isValidMachineOfApp(entity.getApp(), entity.getIp())) {
            return Result.ofFail(Result.CODE_ERROR, "given ip does not belong to given app");
        }
        return null;
    }

    /**
     * 校验 v1 与 v2 流控控制器共有的流控规则特有字段（limitApp、resource、grade、
     * count、strategy/refResource、controlBehavior 及集群配置）；
     * 返回首个错误结果，校验通过返回 null。
     */
    public static <R> Result<R> checkFlowRuleFields(FlowRuleEntity entity) {
        if (StringUtil.isBlank(entity.getLimitApp())) {
            return Result.ofFail(Result.CODE_ERROR, "limitApp can't be null or empty");
        }
        if (StringUtil.isBlank(entity.getResource())) {
            return Result.ofFail(Result.CODE_ERROR, "resource can't be null or empty");
        }
        Integer grade = entity.getGrade();
        if (grade == null) {
            return Result.ofFail(Result.CODE_ERROR, "grade can't be null");
        }
        if (grade != RuleConstant.FLOW_GRADE_THREAD && grade != RuleConstant.FLOW_GRADE_QPS) {
            return Result.ofFail(Result.CODE_ERROR, "grade must be 0 or 1, but " + grade + " got");
        }
        Double count = entity.getCount();
        if (count == null || count < 0) {
            return Result.ofFail(Result.CODE_ERROR, "count should be at lease zero");
        }
        Integer strategy = entity.getStrategy();
        if (strategy == null) {
            return Result.ofFail(Result.CODE_ERROR, "strategy can't be null");
        }
        if (strategy != RuleConstant.STRATEGY_DIRECT && StringUtil.isBlank(entity.getRefResource())) {
            return Result.ofFail(Result.CODE_ERROR, "refResource can't be null or empty when strategy!=0");
        }
        Integer controlBehaviorValue = entity.getControlBehavior();
        if (controlBehaviorValue == null) {
            return Result.ofFail(Result.CODE_ERROR, "controlBehavior can't be null");
        }
        int controlBehavior = controlBehaviorValue;
        if (controlBehavior == RuleConstant.CONTROL_BEHAVIOR_WARM_UP && entity.getWarmUpPeriodSec() == null) {
            return Result.ofFail(Result.CODE_ERROR, "warmUpPeriodSec can't be null when controlBehavior==1");
        }
        if (controlBehavior == RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER && entity.getMaxQueueingTimeMs() == null) {
            return Result.ofFail(Result.CODE_ERROR, "maxQueueingTimeMs can't be null when controlBehavior==2");
        }
        if (entity.isClusterMode() && entity.getClusterConfig() == null) {
            return Result.ofFail(Result.CODE_ERROR, "cluster config should be valid");
        }
        return null;
    }
}
