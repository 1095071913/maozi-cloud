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
 * Base class shared by the flow rule controllers (v1 and v2): holds the common
 * repository/provider/publisher wiring and the common query, add, save/publish
 * and delete/publish logic, so subclasses only keep their API-specific behavior.
 * @author maozi
 * @date 2026/11/22
 */
public abstract class AbstractFlowRuleController {

    /** The logger, named after the concrete controller class. */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /** The in-memory repository that stores flow rule entities. */
    protected final AbstractInMemoryRuleRepository<FlowRuleEntity> repository;
    /** The Nacos provider that pulls flow rules for a given app. */
    protected final DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider;
    /** The Nacos publisher that pushes flow rules to a given app. */
    protected final DynamicRulePublisher<List<FlowRuleEntity>> rulePublisher;

    protected AbstractFlowRuleController(AbstractInMemoryRuleRepository<FlowRuleEntity> repository,
                                         DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider,
                                         DynamicRulePublisher<List<FlowRuleEntity>> rulePublisher) {
        this.repository = repository;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /** Validates the given rule entity; returns the first error result or null when valid. */
    protected abstract <R> Result<R> checkEntityInternal(FlowRuleEntity entity);

    /**
     * Fetches the app's flow rules from Nacos, applies the given stamper to the fetched
     * rules, persists them and returns them.
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
     * Adds a new flow rule after validation (id reset so the repository generates one)
     * and publishes the app's rules.
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

    /** Saves the entity and publishes the app's rules; returns success with the saved entity or a failure result. */
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

    /** Deletes the rule of the given id and publishes the app's rules; an unknown id returns success with null data. */
    protected Result<Long> deleteAndPublish(Long id) {
        FlowRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }
        try {
            repository.delete(id);
            publishRules(oldEntity.getApp());
        } catch (Throwable t) {
            logger.error("Error when deleting flow rule, app={}, id={}", oldEntity.getApp(), id, t);
            return Result.ofFail(Result.CODE_ERROR, t.getMessage());
        }
        return Result.ofSuccess(id);
    }

    /** Publishes all local rules of the app to Nacos. */
    private void publishRules(String app) throws Exception {
        List<FlowRuleEntity> rules = repository.findAllByApp(app);
        rulePublisher.publish(app, rules);
    }
}
