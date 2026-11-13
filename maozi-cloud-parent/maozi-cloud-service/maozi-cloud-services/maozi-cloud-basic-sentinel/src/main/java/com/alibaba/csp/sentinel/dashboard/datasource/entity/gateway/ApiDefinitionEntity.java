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
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.slots.block.Rule;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity for {@link ApiDefinition}.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class ApiDefinitionEntity implements RuleEntity {

    /** The entity id. */
    private Long id;
    /** The application name. */
    private String app;
    /** The IP address of the machine. */
    private String ip;
    /** The port of the machine. */
    private Integer port;

    /** The create time. */
    private Date gmtCreate;
    /** The last modify time. */
    private Date gmtModified;

    /** The name of the API. */
    private String apiName;
    /** The predicate items of the API. */
    private Set<ApiPredicateItemEntity> predicateItems;

    /**
     * Build an entity from the given {@link ApiDefinition}, taking the pattern and
     * matchStrategy of each {@link ApiPathPredicateItem} as predicate items.
     */
    public static ApiDefinitionEntity fromApiDefinition(String app, String ip, Integer port, ApiDefinition apiDefinition) {
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
     * Convert to an {@link ApiDefinition}, with each predicate item converted to an
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

    /** Create an empty entity. */
    public ApiDefinitionEntity() {

    }

    /** Create an entity with the given API name and predicate items. */
    public ApiDefinitionEntity(String apiName, Set<ApiPredicateItemEntity> predicateItems) {
        this.apiName = apiName;
        this.predicateItems = predicateItems;
    }

    /** Get the name of the API. */
    public String getApiName() {
        return apiName;
    }

    /** Set the name of the API. */
    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    /** Get the predicate items of the API. */
    public Set<ApiPredicateItemEntity> getPredicateItems() {
        return predicateItems;
    }

    /** Set the predicate items of the API. */
    public void setPredicateItems(Set<ApiPredicateItemEntity> predicateItems) {
        this.predicateItems = predicateItems;
    }

    /** Get the entity id. */
    @Override
    public Long getId() {
        return id;
    }

    /** Set the entity id. */
    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /** Get the application name. */
    @Override
    public String getApp() {
        return app;
    }

    /** Set the application name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the IP address of the machine. */
    @Override
    public String getIp() {
        return ip;
    }

    /** Set the IP address of the machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Get the port of the machine. */
    @Override
    public Integer getPort() {
        return port;
    }

    /** Set the port of the machine. */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** Get the create time. */
    @Override
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /** Set the create time. */
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /** Get the last modify time. */
    public Date getGmtModified() {
        return gmtModified;
    }

    /** Set the last modify time. */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    /** Always return null, as an API definition is not a common Sentinel rule. */
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

    /** Get the hash code from all fields of this entity. */
    @Override
    public int hashCode() {
        return Objects.hash(id, app, ip, port, gmtCreate, gmtModified, apiName, predicateItems);
    }

    /** Build a string with all fields of this entity. */
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
