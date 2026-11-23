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
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.AbstractInMemoryRuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * 流控规则控制器（v1 与 v2）的公共基类：持有仓储/拉取/推送的公共装配，
 * 并封装查询、新增、保存推送、删除推送的通用逻辑，子类只保留各自的 API 差异。
 * @author maozi
 * @date 2026/11/22
 */
public abstract class AbstractFlowRuleController {

    /** 日志记录器，以具体控制器类命名。 */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /** 存储流控规则实体的内存仓储。 */
    protected final AbstractInMemoryRuleRepository<FlowRuleEntity> repository;
    /** 从 Nacos 拉取指定应用流控规则的提供者。 */
    protected final DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider;
    /** 向指定应用推送流控规则的发布者。 */
    protected final DynamicRulePublisher<List<FlowRuleEntity>> rulePublisher;

    /** 注入流控规则仓储、规则提供者与规则发布者。 */
    protected AbstractFlowRuleController(AbstractInMemoryRuleRepository<FlowRuleEntity> repository,
                                         DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider,
                                         DynamicRulePublisher<List<FlowRuleEntity>> rulePublisher) {
        this.repository = repository;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /**
     * 校验指定规则实体；返回首个错误结果，校验通过返回 null。
     *
     * @param entity 待校验的规则实体
     * @param <R>    结果数据类型
     * @return 错误结果，校验通过返回 null
     */
    protected abstract <R> Result<R> checkEntityInternal(FlowRuleEntity entity);

    /**
     * 从 Nacos 拉取应用的流控规则，对拉取结果应用指定的附加处理，
     * 持久化后返回。
     *
     * @param app     应用名
     * @param stamper 对规则列表的附加处理（如补充时间戳）
     * @return 拉取并保存后的规则列表结果
     */
    protected Result<List<FlowRuleEntity>> fetchAndSaveRules(String app, Consumer<List<FlowRuleEntity>> stamper) {
        try {
            List<FlowRuleEntity> rules = ruleProvider.getRules(app);
            if (rules != null && !rules.isEmpty()) {
                stamper.accept(rules);
            }
            rules = repository.saveAll(rules);
            return Result.ofSuccess(rules);
        } catch (Throwable throwable) {
            logger.error("Error when querying flow rules", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
    }

    /**
     * 校验通过后新增流控规则（重置 id 以便仓储生成），并推送应用的规则。
     *
     * @param entity 待新增的规则实体
     * @return 新增结果
     */
    protected Result<FlowRuleEntity> addFlowRule(FlowRuleEntity entity) {
        Result<FlowRuleEntity> checkResult = checkEntityInternal(entity);
        if (checkResult != null) {
            return checkResult;
        }
        entity.setId(null);
        entity.setGmtCreateAndModified(new Date());
        entity.setLimitApp(entity.getLimitApp().trim());
        entity.setResource(entity.getResource().trim());
        return saveAndPublish(entity);
    }

    /**
     * 保存实体并推送应用的规则；成功返回保存后的实体，失败返回错误结果。
     *
     * @param entity 待保存的规则实体
     * @return 保存并推送后的结果
     */
    protected Result<FlowRuleEntity> saveAndPublish(FlowRuleEntity entity) {
        try {
            FlowRuleEntity saved = repository.save(entity);
            if (saved == null) {
                return Result.ofFail(Result.CODE_ERROR, "save entity fail: null");
            }
            publishRules(saved.getApp());
            return Result.ofSuccess(saved);
        } catch (Throwable t) {
            logger.error("Failed to save flow rule, app={}", entity.getApp(), t);
            return Result.ofFail(Result.CODE_ERROR, t.getMessage());
        }
    }

    /**
     * 删除指定 id 的规则并推送应用的规则；id 不存在时直接成功返回（数据为 null）。
     *
     * @param id 规则 id
     * @return 删除结果
     */
    protected Result<Long> deleteAndPublish(Long id) {
        FlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }
        // 缓存应用名，删除推送与异常日志共用，避免同一方法内重复调用
        String app = oldEntity.getApp();
        try {
            repository.delete(id);
            publishRules(app);
        } catch (Throwable t) {
            logger.error("Error when deleting flow rule, app={}, id={}", app, id, t);
            return Result.ofFail(Result.CODE_ERROR, t.getMessage());
        }
        return Result.ofSuccess(id);
    }

    /**
     * 将应用在本地的全部规则推送至 Nacos。
     *
     * @param app 应用名
     * @throws Exception 推送失败时抛出
     */
    private void publishRules(String app) throws Exception {
        List<FlowRuleEntity> rules = repository.findAllByApp(app);
        rulePublisher.publish(app, rules);
    }
}
