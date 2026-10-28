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
 * Value Object for update gateway api.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class UpdateApiReqVo {

    /** ID of the gateway API definition to update. */
    private Long id;

    /** Name of the target application. */
    private String app;

    /** New predicate items (URL match conditions) of the gateway API definition. */
    private List<ApiPredicateItemVo> predicateItems;

    /** Returns the ID of the gateway API definition to update. */
    public Long getId() {
        return id;
    }

    /** Sets the ID of the gateway API definition to update. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Returns the name of the target application. */
    public String getApp() {
        return app;
    }

    /** Sets the name of the target application. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Returns the new predicate items of the gateway API definition. */
    public List<ApiPredicateItemVo> getPredicateItems() {
        return predicateItems;
    }

    /** Sets the new predicate items of the gateway API definition. */
    public void setPredicateItems(List<ApiPredicateItemVo> predicateItems) {
        this.predicateItems = predicateItems;
    }
}
