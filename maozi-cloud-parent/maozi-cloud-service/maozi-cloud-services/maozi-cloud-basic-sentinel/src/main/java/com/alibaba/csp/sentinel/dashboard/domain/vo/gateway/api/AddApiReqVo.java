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
 * Value Object for add gateway api.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class AddApiReqVo {

    /** Name of the target application. */
    private String app;

    /** IP of the Sentinel client machine that the API definition belongs to. */
    private String ip;

    /** Port of the Sentinel client machine that the API definition belongs to. */
    private Integer port;

    /** Name of the gateway API definition. */
    private String apiName;

    /** Predicate items (URL match conditions) of the gateway API definition. */
    private List<ApiPredicateItemVo> predicateItems;

    /** Returns the name of the target application. */
    public String getApp() {
        return app;
    }

    /** Sets the name of the target application. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Returns the IP of the Sentinel client machine. */
    public String getIp() {
        return ip;
    }

    /** Sets the IP of the Sentinel client machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Returns the port of the Sentinel client machine. */
    public Integer getPort() {
        return port;
    }

    /** Sets the port of the Sentinel client machine. */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** Returns the name of the gateway API definition. */
    public String getApiName() {
        return apiName;
    }

    /** Sets the name of the gateway API definition. */
    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    /** Returns the predicate items of the gateway API definition. */
    public List<ApiPredicateItemVo> getPredicateItems() {
        return predicateItems;
    }

    /** Sets the predicate items of the gateway API definition. */
    public void setPredicateItems(List<ApiPredicateItemVo> predicateItems) {
        this.predicateItems = predicateItems;
    }
}

