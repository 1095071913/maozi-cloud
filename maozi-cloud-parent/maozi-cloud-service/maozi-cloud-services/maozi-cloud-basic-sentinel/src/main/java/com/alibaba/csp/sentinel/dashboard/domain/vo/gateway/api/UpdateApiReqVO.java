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
package com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.api;

import java.util.List;

/**
 * 更新网关 API 的请求视图对象。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class UpdateApiReqVO {

    /** 待更新的网关 API 定义 id。 */
    private Long id;

    /** 目标应用名。 */
    private String app;

    /** 网关 API 定义的新断言项（URL 匹配条件）列表。 */
    private List<ApiPredicateItemVO> predicateItems;

    /** 获取待更新的网关 API 定义 id。 */
    public Long getId() {
        return id;
    }

    /** 设置待更新的网关 API 定义 id。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取目标应用名。 */
    public String getApp() {
        return app;
    }

    /** 设置目标应用名。 */
    public void setApp(String app) {
        this.app = app;
    }

    /** 获取网关 API 定义的新断言项列表。 */
    public List<ApiPredicateItemVO> getPredicateItems() {
        return predicateItems;
    }

    /** 设置网关 API 定义的新断言项列表。 */
    public void setPredicateItems(List<ApiPredicateItemVO> predicateItems) {
        this.predicateItems = predicateItems;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "UpdateApiReqVO{"
                + "id=" + id
                + ", app='" + app + '\''
                + ", predicateItems=" + predicateItems
                + '}';
    }
}
