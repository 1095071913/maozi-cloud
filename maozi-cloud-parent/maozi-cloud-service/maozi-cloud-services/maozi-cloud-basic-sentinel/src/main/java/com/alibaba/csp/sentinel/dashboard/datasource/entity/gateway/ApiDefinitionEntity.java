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
package com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.BaseRuleEntity;
import com.alibaba.csp.sentinel.slots.block.Rule;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * {@link ApiDefinition} 对应的网关 API 分组实体。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class ApiDefinitionEntity extends BaseRuleEntity {

    /** API 名称。 */
    private String apiName;
    /** API 的断言条目集合。 */
    private Set<ApiPredicateItemEntity> predicateItems;

    /**
     * 由指定 {@link ApiDefinition} 构建实体，取每个 {@link ApiPathPredicateItem}
     * 的 pattern 与 matchStrategy 作为断言条目。
     */
    public static ApiDefinitionEntity fromApiDefinition(String app, String ip, Integer port, 
        ApiDefinition apiDefinition) {
        ApiDefinitionEntity entity = new ApiDefinitionEntity();
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        entity.setApiName(apiDefinition.getApiName());

        Set<ApiPredicateItemEntity> predicateItems = new LinkedHashSet<>();
        entity.setPredicateItems(predicateItems);

        Set<ApiPredicateItem> apiPredicateItems = apiDefinition.getPredicateItems();
        if (apiPredicateItems != null) {
            for (ApiPredicateItem apiPredicateItem : apiPredicateItems) {
                ApiPredicateItemEntity itemEntity = new ApiPredicateItemEntity();
                predicateItems.add(itemEntity);
                ApiPathPredicateItem pathPredicateItem = (ApiPathPredicateItem) apiPredicateItem;
                itemEntity.setPattern(pathPredicateItem.getPattern());
                itemEntity.setMatchStrategy(pathPredicateItem.getMatchStrategy());
            }
        }

        return entity;
    }

    /**
     * 转换为 {@link ApiDefinition}，每个断言条目转换为
     * {@link ApiPathPredicateItem}。
     */
    public ApiDefinition toApiDefinition() {
        ApiDefinition apiDefinition = new ApiDefinition();
        apiDefinition.setApiName(apiName);

        Set<ApiPredicateItem> apiPredicateItems = new LinkedHashSet<>();
        apiDefinition.setPredicateItems(apiPredicateItems);

        if (predicateItems != null) {
            for (ApiPredicateItemEntity predicateItem : predicateItems) {
                ApiPathPredicateItem apiPredicateItem = new ApiPathPredicateItem();
                apiPredicateItems.add(apiPredicateItem);
                apiPredicateItem.setMatchStrategy(predicateItem.getMatchStrategy());
                apiPredicateItem.setPattern(predicateItem.getPattern());
            }
        }

        return apiDefinition;
    }

    /** 构造空实体。 */
    public ApiDefinitionEntity() {

    }

    /** 以指定 API 名称与断言条目构造实体。 */
    public ApiDefinitionEntity(String apiName, Set<ApiPredicateItemEntity> predicateItems) {
        this.apiName = apiName;
        this.predicateItems = predicateItems;
    }

    /** 获取 API 名称。 */
    public String getApiName() {
        return apiName;
    }

    /** 设置 API 名称。 */
    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    /** 获取 API 的断言条目集合。 */
    public Set<ApiPredicateItemEntity> getPredicateItems() {
        return predicateItems;
    }

    /** 设置 API 的断言条目集合。 */
    public void setPredicateItems(Set<ApiPredicateItemEntity> predicateItems) {
        this.predicateItems = predicateItems;
    }

    /** 始终返回 null，因为 API 分组不是通用 Sentinel 规则。 */
    @Override
    public Rule toRule() {
        return null;
    }

    /** 判断指定对象是否与本实体全部字段相同。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ApiDefinitionEntity entity = (ApiDefinitionEntity) o;
        return Objects.equals(id, entity.id) &&
                Objects.equals(app, entity.app) &&
                Objects.equals(ip, entity.ip) &&
                Objects.equals(port, entity.port) &&
                Objects.equals(gmtCreate, entity.gmtCreate) &&
                Objects.equals(gmtModified, entity.gmtModified) &&
                Objects.equals(apiName, entity.apiName) &&
                Objects.equals(predicateItems, entity.predicateItems);
    }

    /** 以全部字段计算哈希码。 */
    @Override
    public int hashCode() {
        return Objects.hash(id, app, ip, port, gmtCreate, gmtModified, apiName, predicateItems);
    }

    /** 拼接全部字段构建字符串。 */
    @Override
    public String toString() {
        return "ApiDefinitionEntity{" +
                "id=" + id +
                ", app='" + app + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                ", apiName='" + apiName + '\'' +
                ", predicateItems=" + predicateItems +
                '}';
    }
}
