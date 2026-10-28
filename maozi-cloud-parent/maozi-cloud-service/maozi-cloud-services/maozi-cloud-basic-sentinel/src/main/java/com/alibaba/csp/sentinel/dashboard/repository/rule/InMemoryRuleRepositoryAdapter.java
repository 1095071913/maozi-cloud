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
package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.util.AssertUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leyou
 */
public abstract class InMemoryRuleRepositoryAdapter<T extends RuleEntity> implements RuleRepository<T, Long> {

    /**
     * {@code <machine, <id, rule>>}
     */
    private Map<MachineInfo, Map<Long, T>> machineRules = new ConcurrentHashMap<>(16);
    /** All stored rules keyed by rule id ({@code <id, rule>}). */
    private Map<Long, T> allRules = new ConcurrentHashMap<>(16);

    /** Rules grouped by application name ({@code <app, <id, rule>>}). */
    private Map<String, Map<Long, T>> appRules = new ConcurrentHashMap<>(16);

    /** Maximum number of rules this repository may hold; not enforced by the current code. */
    private static final int MAX_RULES_SIZE = 10000;

    /**
     * Save the given entity, assigning it a generated id when it has none. The entity is
     * first passed to {@link #preProcess}; when the processed entity is non-null it is put
     * into all three indexes (by id, by machine and by app), otherwise nothing is stored
     * and {@code null} is returned.
     */
    @Override
    public T save(T entity) {
        if (entity.getId() == null) {
            entity.setId(nextId());
        }
        T processedEntity = preProcess(entity);
        if (processedEntity != null) {
            allRules.put(processedEntity.getId(), processedEntity);
            machineRules.computeIfAbsent(MachineInfo.of(processedEntity.getApp(), processedEntity.getIp(),
                processedEntity.getPort()), e -> new ConcurrentHashMap<>(32))
                .put(processedEntity.getId(), processedEntity);
            appRules.computeIfAbsent(processedEntity.getApp(), v -> new ConcurrentHashMap<>(32))
                .put(processedEntity.getId(), processedEntity);
        }

        return processedEntity;
    }

    /**
     * Replace all stored rules with the given list: every existing rule in all three
     * indexes is cleared first, then each rule is saved one by one via {@link #save};
     * returns {@code null} when the given list is {@code null}.
     */
    @Override
    public List<T> saveAll(List<T> rules) {
        // TODO: check here.
        allRules.clear();
        machineRules.clear();
        appRules.clear();

        if (rules == null) {
            return null;
        }
        List<T> savedRules = new ArrayList<>(rules.size());
        for (T rule : rules) {
            savedRules.add(save(rule));
        }
        return savedRules;
    }

    /**
     * Remove the rule with the given id from all three indexes and return it, or
     * {@code null} if no rule with the id exists.
     */
    @Override
    public T delete(Long id) {
        T entity = allRules.remove(id);
        if (entity != null) {
            if (appRules.get(entity.getApp()) != null) {
                appRules.get(entity.getApp()).remove(id);
            }
            machineRules.get(MachineInfo.of(entity.getApp(), entity.getIp(), entity.getPort())).remove(id);
        }
        return entity;
    }

    /** Returns the rule with the given id, or {@code null} if it does not exist. */
    @Override
    public T findById(Long id) {
        return allRules.get(id);
    }

    /** Returns all rules bound to the given machine, or an empty list if none exist. */
    @Override
    public List<T> findAllByMachine(MachineInfo machineInfo) {
        Map<Long, T> entities = machineRules.get(machineInfo);
        if (entities == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(entities.values());
    }

    /**
     * Returns all rules of the given application, or an empty list if the app is unknown;
     * appName must not be empty.
     */
    @Override
    public List<T> findAllByApp(String appName) {
        AssertUtil.notEmpty(appName, "appName cannot be empty");
        Map<Long, T> entities = appRules.get(appName);
        if (entities == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(entities.values());
    }

    /** Removes all rules from all three indexes. */
    public void clearAll() {
        allRules.clear();
        machineRules.clear();
        appRules.clear();
    }

    /**
     * Hook for subclasses to transform or validate an entity before it is stored;
     * returning {@code null} skips storing it. The default implementation returns the
     * entity unchanged.
     */
    protected T preProcess(T entity) {
        return entity;
    }

    /**
     * Get next unused id.
     *
     * @return next unused id
     */
    abstract protected long nextId();
}
