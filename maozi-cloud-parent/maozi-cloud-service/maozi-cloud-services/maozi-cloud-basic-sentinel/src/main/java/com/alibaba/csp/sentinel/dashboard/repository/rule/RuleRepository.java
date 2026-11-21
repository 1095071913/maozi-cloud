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

import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;

import java.util.List;

/**
 * 规则的存储与查询接口
 *
 * @author leyou
 * @date 2026/11/22
 */
public interface RuleRepository<T, ID> {

    /**
     * 保存给定的规则实体
     *
     * @param entity 待保存的实体
     * @return 保存后的实体
     */
    T save(T entity);

    /**
     * 批量保存给定的规则
     *
     * @param rules 待保存的规则列表
     * @return 保存后的规则列表
     */
    List<T> saveAll(List<T> rules);

    /**
     * 删除指定 id 的规则
     *
     * @param id 待删除实体的 id
     * @return 被删除的实体，id 不存在时返回 {@code null}
     */
    T delete(ID id);

    /**
     * 查询指定 id 的规则
     *
     * @param id 实体 id
     * @return 对应 id 的实体，不存在时返回 {@code null}
     */
    T findById(ID id);

    /**
     * 查询绑定到指定机器的全部规则
     *
     * @param machineInfo 待查询规则的机器
     * @return 绑定到该机器的规则
     */
    List<T> findAllByMachine(MachineInfo machineInfo);

    /**
     * 查询指定应用的全部规则
     *
     * @param appName 应用名，不能为空
     * @return 该应用的全部规则
     * @since 1.4.0
     */
    List<T> findAllByApp(String appName);

}
