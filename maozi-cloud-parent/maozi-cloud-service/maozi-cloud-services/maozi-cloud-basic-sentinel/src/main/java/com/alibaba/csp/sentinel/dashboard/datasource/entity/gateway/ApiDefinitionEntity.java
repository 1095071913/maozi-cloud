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
 * Entity for {@link ApiDefinition}.
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class ApiDefinitionEntity extends BaseRuleEntity {

    /** The name of the API. */
    private String apiName;
    /** The predicate items of the API. */
    private Set<ApiPredicateItemEntity> predicateItems;

    /**
     * Builds an entity from the given {@link ApiDefinition}, taking the pattern and
     * matchStrategy of each {@link ApiPathPredicateItem} as predicate items.
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
     * Converts to an {@link ApiDefinition}, with each predicate item converted to an
     * {@link ApiPathPredicateItem}.
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

    /** Creates an empty entity. */
    public ApiDefinitionEntity() {

    }

    /** Creates an entity with the given API name and predicate items. */
    public ApiDefinitionEntity(String apiName, Set<ApiPredicateItemEntity> predicateItems) {
        this.apiName = apiName;
        this.predicateItems = predicateItems;
    }

    /** Gets the name of the API. */
    public String getApiName() {
        return apiName;
    }

    /** Sets the name of the API. */
    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    /** Gets the predicate items of the API. */
    public Set<ApiPredicateItemEntity> getPredicateItems() {
        return predicateItems;
    }

    /** Sets the predicate items of the API. */
    public void setPredicateItems(Set<ApiPredicateItemEntity> predicateItems) {
        this.predicateItems = predicateItems;
    }

    /** Always returns null, as an API definition is not a common Sentinel rule. */
    @Override
    public Rule toRule() {
        return null;
    }

    /** Whether the given object has the same fields as this entity. */
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

    /** Gets the hash code from all fields of this entity. */
    @Override
    public int hashCode() {
        return Objects.hash(id, app, ip, port, gmtCreate, gmtModified, apiName, predicateItems);
    }

    /** Builds a string with all fields of this entity. */
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
