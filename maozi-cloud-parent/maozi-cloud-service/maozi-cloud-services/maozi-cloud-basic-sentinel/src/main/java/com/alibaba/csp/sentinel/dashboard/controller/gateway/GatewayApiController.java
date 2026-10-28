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
package com.alibaba.csp.sentinel.dashboard.controller.gateway;

import com.alibaba.csp.sentinel.dashboard.auth.AuthAction;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiPredicateItemEntity;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.api.AddApiReqVo;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.api.ApiPredicateItemVo;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.api.UpdateApiReqVo;
import com.alibaba.csp.sentinel.dashboard.repository.gateway.InMemApiDefinitionStore;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX;
import static com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.URL_MATCH_STRATEGY_REGEX;

/**
 * Gateway api Controller for manage gateway api definitions.
 *
 * @author cdfive
 * @since 1.7.0
 */
@RestController
@RequestMapping(value = "/gateway/api")
public class GatewayApiController {

    /** The logger of this controller. */
    private final Logger logger = LoggerFactory.getLogger(GatewayApiController.class);

    /** The in-memory store that keeps gateway API definitions. */
    @Autowired
    private InMemApiDefinitionStore repository;

    /** The Nacos provider that pulls gateway API definitions for a given app. */
    @Autowired
    @Qualifier("apiDefinitionNacosProvider")
    private DynamicRuleProvider<List<ApiDefinitionEntity>> ruleProvider;
    /** The Nacos publisher that pushes gateway API definitions to a given app. */
    @Autowired
    @Qualifier("apiDefinitionNacosPublisher")
    private DynamicRulePublisher<List<ApiDefinitionEntity>> rulePublisher;

    /** Fetches the app's gateway API definitions from Nacos, stamps machine info, persists them and returns them. */
    @GetMapping("/list.json")
    @AuthAction(AuthService.PrivilegeType.READ_RULE)
    public Result<List<ApiDefinitionEntity>> queryApis(String app, String ip, Integer port) {

        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(ip)) {
            return Result.ofFail(-1, "ip can't be null or empty");
        }
        if (port == null) {
            return Result.ofFail(-1, "port can't be null");
        }

        try {
            List<ApiDefinitionEntity> apis = ruleProvider.getRules(app);
            for (ApiDefinitionEntity entity : apis) {
                entity.setApp(app);
                entity.setIp(ip);
                entity.setPort(port);
            }
            apis = repository.saveAll(apis);
            return Result.ofSuccess(apis);
        } catch (Throwable throwable) {
            logger.error("queryApis error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }
    }

    /** Adds a new API definition after validation (duplicate apiName within the app rejected), then publishes the app's APIs to Nacos. */
    @PostMapping("/new.json")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<ApiDefinitionEntity> addApi(HttpServletRequest request, @RequestBody AddApiReqVo reqVo) {

        String app = reqVo.getApp();
        if (StringUtil.isBlank(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }

        ApiDefinitionEntity entity = new ApiDefinitionEntity();
        entity.setApp(app.trim());

        String ip = reqVo.getIp();
        if (StringUtil.isBlank(ip)) {
            return Result.ofFail(-1, "ip can't be null or empty");
        }
        entity.setIp(ip.trim());

        Integer port = reqVo.getPort();
        if (port == null) {
            return Result.ofFail(-1, "port can't be null");
        }
        entity.setPort(port);

        // API名称
        String apiName = reqVo.getApiName();
        if (StringUtil.isBlank(apiName)) {
            return Result.ofFail(-1, "apiName can't be null or empty");
        }
        entity.setApiName(apiName.trim());

        // 匹配规则列表
        List<ApiPredicateItemVo> predicateItems = reqVo.getPredicateItems();
        if (CollectionUtils.isEmpty(predicateItems)) {
            return Result.ofFail(-1, "predicateItems can't empty");
        }

        List<ApiPredicateItemEntity> predicateItemEntities = new ArrayList<>();
        for (ApiPredicateItemVo predicateItem : predicateItems) {
            ApiPredicateItemEntity predicateItemEntity = new ApiPredicateItemEntity();

            // 匹配模式
            Integer matchStrategy = predicateItem.getMatchStrategy();
            if (!Arrays.asList(URL_MATCH_STRATEGY_EXACT, URL_MATCH_STRATEGY_PREFIX, URL_MATCH_STRATEGY_REGEX).contains(matchStrategy)) {
                return Result.ofFail(-1, "invalid matchStrategy: " + matchStrategy);
            }
            predicateItemEntity.setMatchStrategy(matchStrategy);

            // 匹配串
            String pattern = predicateItem.getPattern();
            if (StringUtil.isBlank(pattern)) {
                return Result.ofFail(-1, "pattern can't be null or empty");
            }
            predicateItemEntity.setPattern(pattern);

            predicateItemEntities.add(predicateItemEntity);
        }
        entity.setPredicateItems(new LinkedHashSet<>(predicateItemEntities));

        // 检查API名称不能重复
        List<ApiDefinitionEntity> allApis = repository.findAllByApp(app.trim());
        if (allApis.stream().map(o -> o.getApiName()).anyMatch(o -> o.equals(apiName.trim()))) {
            return Result.ofFail(-1, "apiName exists: " + apiName);
        }

        Date date = new Date();
        entity.setGmtCreate(date);
        entity.setGmtModified(date);

        try {
            entity = repository.save(entity);
        } catch (Throwable throwable) {
            logger.error("add gateway api error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }

        if (!publishApis(app)) {
            logger.warn("publish gateway apis fail after add");
        }

        return Result.ofSuccess(entity);
    }

    /** Updates the predicate items of the API definition with the given id, then publishes the app's APIs to Nacos. */
    @PostMapping("/save.json")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<ApiDefinitionEntity> updateApi(@RequestBody UpdateApiReqVo reqVo) {
        String app = reqVo.getApp();
        if (StringUtil.isBlank(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }

        Long id = reqVo.getId();
        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }

        ApiDefinitionEntity entity = repository.findById(id);
        if (entity == null) {
            return Result.ofFail(-1, "api does not exist, id=" + id);
        }

        // 匹配规则列表
        List<ApiPredicateItemVo> predicateItems = reqVo.getPredicateItems();
        if (CollectionUtils.isEmpty(predicateItems)) {
            return Result.ofFail(-1, "predicateItems can't empty");
        }

        List<ApiPredicateItemEntity> predicateItemEntities = new ArrayList<>();
        for (ApiPredicateItemVo predicateItem : predicateItems) {
            ApiPredicateItemEntity predicateItemEntity = new ApiPredicateItemEntity();

            // 匹配模式
            int matchStrategy = predicateItem.getMatchStrategy();
            if (!Arrays.asList(URL_MATCH_STRATEGY_EXACT, URL_MATCH_STRATEGY_PREFIX, URL_MATCH_STRATEGY_REGEX).contains(matchStrategy)) {
                return Result.ofFail(-1, "Invalid matchStrategy: " + matchStrategy);
            }
            predicateItemEntity.setMatchStrategy(matchStrategy);

            // 匹配串
            String pattern = predicateItem.getPattern();
            if (StringUtil.isBlank(pattern)) {
                return Result.ofFail(-1, "pattern can't be null or empty");
            }
            predicateItemEntity.setPattern(pattern);

            predicateItemEntities.add(predicateItemEntity);
        }
        entity.setPredicateItems(new LinkedHashSet<>(predicateItemEntities));

        Date date = new Date();
        entity.setGmtModified(date);

        try {
            entity = repository.save(entity);
        } catch (Throwable throwable) {
            logger.error("update gateway api error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }

        if (!publishApis(app)) {
            logger.warn("publish gateway apis fail after update");
        }

        return Result.ofSuccess(entity);
    }

    /** Deletes the API definition of the given id and publishes the app's APIs; an unknown id returns success with null data. */
    @PostMapping("/delete.json")
    @AuthAction(AuthService.PrivilegeType.DELETE_RULE)

    public Result<Long> deleteApi(Long id) {
        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }

        ApiDefinitionEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }

        try {
            repository.delete(id);
        } catch (Throwable throwable) {
            logger.error("delete gateway api error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }

        if (!publishApis(oldEntity.getApp())) {
            logger.warn("publish gateway apis fail after delete");
        }

        return Result.ofSuccess(id);
    }

    /** Publishes all local API definitions of the app to Nacos; returns false on error. */
    private boolean publishApis(String app) {
        try {
            List<ApiDefinitionEntity> apis = repository.findAllByApp(app);
            rulePublisher.publish(app, apis);
            return true;
        } catch (Exception e) {
            logger.error("Error when publishing gateway apis to Nacos, app={}", app, e);
            return false;
        }
    }
}
