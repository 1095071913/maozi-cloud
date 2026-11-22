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
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.api.AddApiReqVO;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.api.ApiPredicateItemVO;
import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.api.UpdateApiReqVO;
import com.alibaba.csp.sentinel.dashboard.repository.gateway.InMemApiDefinitionStore;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 网关 API 分组管理控制器。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
@RestController
@RequestMapping(value = "/gateway/api")
public class GatewayApiController {

    /** 日志记录器。 */
    private final Logger logger = LoggerFactory.getLogger(GatewayApiController.class);

    /** 保存网关 API 分组的内存仓储。 */
    private final InMemApiDefinitionStore repository;

    /** 从 Nacos 拉取指定应用网关 API 分组的提供者。 */
    private final DynamicRuleProvider<List<ApiDefinitionEntity>> ruleProvider;
    /** 向指定应用推送网关 API 分组的发布者。 */
    private final DynamicRulePublisher<List<ApiDefinitionEntity>> rulePublisher;

    /** 注入 API 分组仓储、规则提供者与发布者。 */
    public GatewayApiController(InMemApiDefinitionStore repository,
                                @Qualifier("apiDefinitionNacosProvider") DynamicRuleProvider<List<ApiDefinitionEntity>>
                                    ruleProvider,
                                @Qualifier("apiDefinitionNacosPublisher")
                                    DynamicRulePublisher<List<ApiDefinitionEntity>> rulePublisher) {
        this.repository = repository;
        this.ruleProvider = ruleProvider;
        this.rulePublisher = rulePublisher;
    }

    /** 从 Nacos 拉取应用的网关 API 分组，为每条补充机器信息后持久化并返回。 */
    @GetMapping("/list.json")
    @AuthAction(AuthService.PrivilegeType.READ_RULE)
    public Result<List<ApiDefinitionEntity>> queryApis(String app, String ip, Integer port) {

        String error = checkAppIpPort(app, ip, port);
        if (error != null) {
            return Result.ofFail(Result.CODE_ERROR, error);
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
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }
    }

    /**
     * 校验通过后新增 API 分组（应用内重复 apiName 被拒绝），随后将应用的
     * API 分组推送至 Nacos。
     */
    @PostMapping("/new.json")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<ApiDefinitionEntity> addApi(HttpServletRequest request, @RequestBody AddApiReqVO reqVo) {

        String error = checkAppIpPort(reqVo.getApp(), reqVo.getIp(), reqVo.getPort());
        if (error != null) {
            return Result.ofFail(Result.CODE_ERROR, error);
        }
        String trimmedApp = reqVo.getApp().trim();

        // API 名称
        String apiName = reqVo.getApiName();
        if (StringUtil.isBlank(apiName)) {
            return Result.ofFail(Result.CODE_ERROR, "apiName can't be null or empty");
        }

        ApiDefinitionEntity entity = new ApiDefinitionEntity();
        entity.setApp(trimmedApp);
        entity.setIp(reqVo.getIp().trim());
        entity.setPort(reqVo.getPort());
        entity.setApiName(apiName.trim());

        // 断言条目
        List<ApiPredicateItemEntity> predicateItemEntities;
        try {
            predicateItemEntities = toPredicateItemEntities(reqVo.getPredicateItems());
        } catch (IllegalArgumentException e) {
            return Result.ofFail(Result.CODE_ERROR, e.getMessage());
        }
        entity.setPredicateItems(new LinkedHashSet<>(predicateItemEntities));

        // 应用内 API 名称重复则拒绝
        List<ApiDefinitionEntity> allApis = repository.findAllByApp(trimmedApp);
        if (allApis.stream().map(ApiDefinitionEntity::getApiName).anyMatch(apiName.trim()::equals)) {
            return Result.ofFail(Result.CODE_ERROR, "apiName exists: " + apiName);
        }

        entity.setGmtCreateAndModified(new Date());

        return saveAndPublish(entity, reqVo.getApp(), "add");
    }

    /** 更新指定 id 的 API 分组的断言条目，随后将应用的 API 分组推送至 Nacos。 */
    @PostMapping("/save.json")
    @AuthAction(AuthService.PrivilegeType.WRITE_RULE)
    public Result<ApiDefinitionEntity> updateApi(@RequestBody UpdateApiReqVO reqVo) {
        String app = reqVo.getApp();
        if (StringUtil.isBlank(app)) {
            return Result.ofFail(Result.CODE_ERROR, "app can't be null or empty");
        }

        Long id = reqVo.getId();
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }

        ApiDefinitionEntity entity = repository.findById(id);
        if (entity == null) {
            return Result.ofFail(Result.CODE_ERROR, "api does not exist, id=" + id);
        }

        // 断言条目
        List<ApiPredicateItemEntity> predicateItemEntities;
        try {
            predicateItemEntities = toPredicateItemEntities(reqVo.getPredicateItems());
        } catch (IllegalArgumentException e) {
            return Result.ofFail(Result.CODE_ERROR, e.getMessage());
        }
        entity.setPredicateItems(new LinkedHashSet<>(predicateItemEntities));

        entity.setGmtModified(new Date());

        return saveAndPublish(entity, app, "update");
    }

    /** 删除指定 id 的 API 分组并推送应用的 API 分组；id 不存在时直接成功返回（数据为 null）。 */
    @PostMapping("/delete.json")
    @AuthAction(AuthService.PrivilegeType.DELETE_RULE)
    public Result<Long> deleteApi(Long id) {
        if (id == null) {
            return Result.ofFail(Result.CODE_ERROR, "id can't be null");
        }

        ApiDefinitionEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }

        try {
            repository.delete(id);
        } catch (Throwable throwable) {
            logger.error("delete gateway api error:", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }

        if (!publishApis(oldEntity.getApp())) {
            logger.warn("publish gateway apis fail after delete");
        }

        return Result.ofSuccess(id);
    }

    /** 校验 app、ip、port 三个参数，全部合法返回 null，否则返回错误信息。 */
    private String checkAppIpPort(String app, String ip, Integer port) {
        if (StringUtil.isBlank(app)) {
            return "app can't be null or empty";
        }
        if (StringUtil.isBlank(ip)) {
            return "ip can't be null or empty";
        }
        if (port == null) {
            return "port can't be null";
        }
        return null;
    }

    /** 校验并转换断言条目 VO 列表为实体列表，非法时抛出携带错误信息的 {@link IllegalArgumentException}。 */
    private List<ApiPredicateItemEntity> toPredicateItemEntities(List<ApiPredicateItemVO> predicateItems) {
        if (CollectionUtils.isEmpty(predicateItems)) {
            throw new IllegalArgumentException("predicateItems can't empty");
        }

        List<ApiPredicateItemEntity> entities = new ArrayList<>(predicateItems.size());
        for (ApiPredicateItemVO predicateItem : predicateItems) {
            // 匹配策略
            Integer matchStrategy = predicateItem.getMatchStrategy();
            if (!Arrays.asList(URL_MATCH_STRATEGY_EXACT, URL_MATCH_STRATEGY_PREFIX,
                URL_MATCH_STRATEGY_REGEX).contains(matchStrategy)) {
                throw new IllegalArgumentException("invalid matchStrategy: " + matchStrategy);
            }

            // 匹配模式
            String pattern = predicateItem.getPattern();
            if (StringUtil.isBlank(pattern)) {
                throw new IllegalArgumentException("pattern can't be null or empty");
            }

            ApiPredicateItemEntity predicateItemEntity = new ApiPredicateItemEntity();
            predicateItemEntity.setMatchStrategy(matchStrategy);
            predicateItemEntity.setPattern(pattern);
            entities.add(predicateItemEntity);
        }
        return entities;
    }

    /** 保存 API 分组并将应用全部分组推送至 Nacos；action 用于区分日志（add/update）。 */
    private Result<ApiDefinitionEntity> saveAndPublish(ApiDefinitionEntity entity, String app, String action) {
        try {
            entity = repository.save(entity);
        } catch (Throwable throwable) {
            logger.error(action + " gateway api error:", throwable);
            return Result.ofThrowable(Result.CODE_ERROR, throwable);
        }

        if (!publishApis(app)) {
            logger.warn("publish gateway apis fail after " + action);
        }
        return Result.ofSuccess(entity);
    }

    /** 将应用在本地的全部 API 分组推送至 Nacos；出错时返回 false。 */
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
