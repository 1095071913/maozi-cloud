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
 * Interface to store and find rules.
 *
 * @author leyou
 */
public interface RuleRepository<T, ID> {

    /**
     * Save the given rule entity.
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    T save(T entity);

    /**
     * Save all the given rules.
     *
     * @param rules the rules to save
     * @return the saved rules
     */
    List<T> saveAll(List<T> rules);

    /**
     * Delete the rule with the given id.
     *
     * @param id the id of the entity to delete
     * @return the deleted entity, or {@code null} if no rule with the id exists
     */
    T delete(ID id);

    /**
     * Find the rule with the given id.
     *
     * @param id the id of the entity
     * @return the entity with the given id, or {@code null} if it does not exist
     */
    T findById(ID id);

    /**
     * Find all rules bound to the given machine.
     *
     * @param machineInfo the machine to find rules for
     * @return rules bound to the given machine
     */
    List<T> findAllByMachine(MachineInfo machineInfo);

    /**
     * Find all rules of the given application.
     *
     * @param appName application name, must not be empty
     * @return all rules of the application
     * @since 1.4.0
     */
    List<T> findAllByApp(String appName);

}
