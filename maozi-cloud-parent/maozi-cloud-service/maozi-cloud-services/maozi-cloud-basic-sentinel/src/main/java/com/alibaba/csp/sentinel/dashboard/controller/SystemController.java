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
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.RuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 系统自适应保护规则控制器。
 *
 * @author leyou(lihao)
 * @date 2026/11/22
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    /** 日志记录器。 */
    private final Logger logger = LoggerFactory.getLogger(SystemController.class);

    /** 存储系统规则实体的内存仓储。 */
    private final RuleRepository<SystemRuleEntity, Long> repository;
    /** 用于校验机器与应用归属关系的应用管理服务。 */
    private final AppManagement appManagement;
    /** 从 Nacos 拉取指定应用系统规则的提供者。 */
    private final DynamicRuleProvider<List<SystemRuleEntity>> ruleProvider;
    /** 向指定应用推送系统规则的发布者。 */
    private final DynamicRulePublisher<List<SystemRuleEntity>> rulePublisher;

    /** 合法 TCP/UDP 端口的最大值。 */
    private static final int MAX_PORT = 65535;

    /** 注入规则仓储、应用管理、规则提供者与发布者。 */
    public SystemController(RuleRepository<SystemRuleEntity, Long> repository,
                            AppManagement appManagement,
                            @Qualifier("systemRuleNacosProvider") DynamicRuleProvider<List<SystemRuleEntity>>
                                ruleProvider,
                            @Qualifier("systemRuleNacosPublisher") DynamicRulePublisher<List<SystemRuleEntity>>
                                rulePublisher) {
        this.repository = repository;
        this.appManagement = appManagement;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /** 校验 app/ip/port 及 ip 是否属于指定应用；返回首个错误结果，校验通过返回 null。 */
    private <R> Result<R> checkBasicParams(String app, String ip, Integer port) {
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
        if (port <= 0 || port > MAX_PORT) {
            return Result.ofFail(Result.CODE_ERROR, "port should be in (0, 65535)");
        }
        return null;
    }

    /** 从 Nacos 拉取应用的系统规则，为每条补充机器信息后持久化并返回。 */
    @GetMapping("/rules.json")
    @AuthAction(PrivilegeType.READ_RULE)
    public Result<List<SystemRuleEntity>> apiQueryMachineRules(String app, String ip,
                                                               Integer port) {
        Result<List<SystemRuleEntity>> checkResult = checkBasicParams(app, ip, port);
        if (checkResult != null) {
            return checkResult;
        }
        try {
            List<SystemRuleEntity> rules = ruleProvider.getRules(app);
            for (SystemRuleEntity entity : rules) {
                entity.setApp(app);
                entity.setIp(ip);
                entity.setPort(port);
            }
            rules = repository.saveAll(rules);
            return Result.ofSuccess(rules);
        } catch (Throwable throwable) {
            logger.error("Query machine system rules error", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
    }

    /** 统计指定数值中非空且非负的个数。 */
    private int countNotNullAndNotNegative(Number... values) {
        int notNullCount = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && values[i].doubleValue() >= 0) {
                notNullCount++;
            }
        }
        return notNullCount;
    }

    /**
     * 新增系统规则：要求恰好设置一个阈值；未设置的阈值以 -1 持久化
     * （占位值表示“未启用”），随后将应用的规则推送至 Nacos。
     */
    @GetMapping("/new.json")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<SystemRuleEntity> apiAdd(String app, String ip, Integer port,
                                           Double highestSystemLoad, Double highestCpuUsage, Long avgRt,
                                           Long maxThread, Double qps) {

        Result<SystemRuleEntity> checkResult = checkBasicParams(app, ip, port);
        if (checkResult != null) {
            return checkResult;
        }

        int notNullCount = countNotNullAndNotNegative(highestSystemLoad, avgRt, maxThread, qps, highestCpuUsage);
        if (notNullCount != 1) {
            return Result.ofFail(Result.CODE_ERROR,
                "only one of [highestSystemLoad, avgRt, maxThread, qps,highestCpuUsage] "
                + "value must be set > 0, but " + notNullCount + " values get");
        }
        if (null != highestCpuUsage && highestCpuUsage > 1) {
            return Result.ofFail(Result.CODE_ERROR, "highestCpuUsage must between [0.0, 1.0]");
        }
        SystemRuleEntity entity = new SystemRuleEntity();
        entity.setApp(app.trim());
        entity.setIp(ip.trim());
        entity.setPort(port);
        // -1 为占位值，表示该阈值未启用
        if (null != highestSystemLoad) {
            entity.setHighestSystemLoad(highestSystemLoad);
        } else {
            entity.setHighestSystemLoad(-1D);
        }

        if (null != highestCpuUsage) {
            entity.setHighestCpuUsage(highestCpuUsage);
        } else {
            entity.setHighestCpuUsage(-1D);
        }

        if (avgRt != null) {
            entity.setAvgRt(avgRt);
        } else {
            entity.setAvgRt(-1L);
        }
        if (maxThread != null) {
            entity.setMaxThread(maxThread);
        } else {
            entity.setMaxThread(-1L);
        }
        if (qps != null) {
            entity.setQps(qps);
        } else {
            entity.setQps(-1D);
        }
        entity.setGmtCreateAndModified(new Date());
        try {
            entity = repository.save(entity);
        } catch (Throwable throwable) {
            logger.error("Add SystemRule error", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
        if (!publishRules(app)) {
            logger.warn("Publish system rules fail after rule add");
        }
        return Result.ofSuccess(entity);
    }

    /**
     * 更新指定 id 的规则：仅应用非空参数（负阈值被拒绝），并推送应用的规则。
     */
    @GetMapping("/save.json")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<SystemRuleEntity> apiUpdateIfNotNull(Long id, String app, Double highestSystemLoad,
            Double highestCpuUsage, Long avgRt, Long maxThread, Double qps) {
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }
        SystemRuleEntity entity = repository.findById(id);
        if (entity == null) {
            return Result.ofFail(Result.CODE_ERROR, "id " + id + " dose not exist");
        }

        if (StringUtil.isNotBlank(app)) {
            entity.setApp(app.trim());
        }
        if (highestSystemLoad != null) {
            if (highestSystemLoad < 0) {
                return Result.ofFail(Result.CODE_ERROR, "highestSystemLoad must >= 0");
            }
            entity.setHighestSystemLoad(highestSystemLoad);
        }
        if (highestCpuUsage != null) {
            if (highestCpuUsage < 0) {
                return Result.ofFail(Result.CODE_ERROR, "highestCpuUsage must >= 0");
            }
            if (highestCpuUsage > 1) {
                return Result.ofFail(Result.CODE_ERROR, "highestCpuUsage must <= 1");
            }
            entity.setHighestCpuUsage(highestCpuUsage);
        }
        if (avgRt != null) {
            if (avgRt < 0) {
                return Result.ofFail(Result.CODE_ERROR, "avgRt must >= 0");
            }
            entity.setAvgRt(avgRt);
        }
        if (maxThread != null) {
            if (maxThread < 0) {
                return Result.ofFail(Result.CODE_ERROR, "maxThread must >= 0");
            }
            entity.setMaxThread(maxThread);
        }
        if (qps != null) {
            if (qps < 0) {
                return Result.ofFail(Result.CODE_ERROR, "qps must >= 0");
            }
            entity.setQps(qps);
        }
        Date date = new Date();
        entity.setGmtModified(date);
        try {
            entity = repository.save(entity);
        } catch (Throwable throwable) {
            logger.error("save error:", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
        if (!publishRules(entity.getApp())) {
            logger.info("publish system rules fail after rule update");
        }
        return Result.ofSuccess(entity);
    }

    /** 删除指定 id 的规则并推送应用的规则；id 不存在时直接成功返回（数据为 null）。 */
    @GetMapping("/delete.json")
    @AuthAction(PrivilegeType.DELETE_RULE)
    public Result<?> delete(Long id) {
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }
        SystemRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }
        try {
            repository.delete(id);
        } catch (Throwable throwable) {
            logger.error("delete error:", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
        if (!publishRules(oldEntity.getApp())) {
            logger.info("publish system rules fail after rule delete");
        }
        return Result.ofSuccess(id);
    }

    /** 将应用在本地的全部规则推送至 Nacos；出错时返回 false。 */
    private boolean publishRules(String app) {
        try {
            List<SystemRuleEntity> rules = repository.findAllByApp(app);
            rulePublisher.publish(app, rules);
            return true;
        } catch (Exception e) {
            logger.error("Error when publishing system rules to Nacos, app={}", app, e);
            return false;
        }
    }
}
