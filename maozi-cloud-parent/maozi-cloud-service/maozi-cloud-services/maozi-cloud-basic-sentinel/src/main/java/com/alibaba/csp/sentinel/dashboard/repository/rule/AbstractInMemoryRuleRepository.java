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
 * 规则实体的内存仓储抽象基类，按 id、机器与应用三个维度索引
 *
 * @author leyou
 * @date 2026/11/22
 */
public abstract class AbstractInMemoryRuleRepository<T extends RuleEntity> implements RuleRepository<T, Long> {

    /** 仓储可容纳的最大规则数；当前代码并未实际校验该上限 */
    private static final int MAX_RULES_SIZE = 10000;

    /** 集合初始化的默认容量 */
    private static final int INITIAL_CAPACITY = 16;

    /** 按机器分组的规则（{@code <machine, <id, rule>>}） */
    private Map<MachineInfo, Map<Long, T>> machineRules = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    /** 按规则 id 索引的全部规则（{@code <id, rule>}） */
    private Map<Long, T> allRules = new ConcurrentHashMap<>(INITIAL_CAPACITY);

    /** 按应用名分组的规则（{@code <app, <id, rule>>}） */
    private Map<String, Map<Long, T>> appRules = new ConcurrentHashMap<>(INITIAL_CAPACITY);

    /**
     * 保存给定实体，无 id 时为其分配生成的 id。实体先经过 {@link #preProcess} 处理，
     * 处理结果非 null 时写入 id、机器、应用三个索引，否则不存储并返回 {@code null}
     */
    @Override
    public T save(T entity) {
        if (entity.getId() == null) {
            entity.setId(nextId());
        }
        T processedEntity = preProcess(entity);
        if (processedEntity != null) {
            Long entityId = processedEntity.getId();
            String app = processedEntity.getApp();
            allRules.put(entityId, processedEntity);
            machineRules.computeIfAbsent(MachineInfo.of(app, processedEntity.getIp(),
                            processedEntity.getPort()), e -> new ConcurrentHashMap<>(INITIAL_CAPACITY))
                .put(entityId, processedEntity);
            appRules.computeIfAbsent(app, v -> new ConcurrentHashMap<>(INITIAL_CAPACITY))
                    .put(entityId, processedEntity);
        }

        return processedEntity;
    }

    /**
     * 用给定列表替换全部已存规则：先清空三个索引中的所有既有规则，
     * 再逐条通过 {@link #save} 保存；入参为 {@code null} 时返回空列表
     */
    @Override
    public List<T> saveAll(List<T> rules) {
        // TODO maozi 2026/11/21 检查此处逻辑
        allRules.clear();
        machineRules.clear();
        appRules.clear();

        if (rules == null) {
            return new ArrayList<>();
        }
        List<T> savedRules = new ArrayList<>(rules.size());
        for (T rule : rules) {
            savedRules.add(save(rule));
        }
        return savedRules;
    }

    /**
     * 从三个索引中移除指定 id 的规则并返回该规则，
     * id 不存在时返回 {@code null}
     */
    @Override
    public T delete(Long id) {
        T entity = allRules.remove(id);
        if (entity != null) {
            String app = entity.getApp();
            Map<Long, T> appRuleMap = appRules.get(app);
            if (appRuleMap != null) {
                appRuleMap.remove(id);
            }
            machineRules.get(MachineInfo.of(app, entity.getIp(), entity.getPort())).remove(id);
        }
        return entity;
    }

    /** 返回指定 id 的规则，不存在时返回 {@code null} */
    @Override
    public T findById(Long id) {
        return allRules.get(id);
    }

    /** 返回绑定到指定机器的全部规则，无规则时返回空列表 */
    @Override
    public List<T> findAllByMachine(MachineInfo machineInfo) {
        Map<Long, T> entities = machineRules.get(machineInfo);
        if (entities == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(entities.values());
    }

    /**
     * 返回指定应用的全部规则，应用不存在时返回空列表；
     * appName 不能为空
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

    /** 清空三个索引中的全部规则 */
    public void clearAll() {
        allRules.clear();
        machineRules.clear();
        appRules.clear();
    }

    /**
     * 供子类在实体存储前进行转换或校验的钩子方法；
     * 返回 {@code null} 表示跳过存储。默认实现原样返回实体
     */
    protected T preProcess(T entity) {
        return entity;
    }

    /**
     * 获取下一个未使用的 id
     *
     * @return 下一个未使用的 id
     */
    protected abstract long nextId();
}
