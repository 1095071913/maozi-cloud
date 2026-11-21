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
package com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule;

import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.BasePatternItemVO;

/**
 * Hotspot parameter item of a gateway flow rule, used by add and update requests.
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class GatewayParamFlowItemVO extends BasePatternItemVO {

    /** Strategy to parse the parameter from the gateway request (URL param, host, header or cookie). */
    private Integer parseStrategy;

    /** Name of the parsed field, e.g. header name or parameter name, depending on parseStrategy. */
    private String fieldName;

    /** Returns the parameter parse strategy. */
    public Integer getParseStrategy() {
        return parseStrategy;
    }

    /** Sets the parameter parse strategy. */
    public void setParseStrategy(Integer parseStrategy) {
        this.parseStrategy = parseStrategy;
    }

    /** Returns the name of the parsed field. */
    public String getFieldName() {
        return fieldName;
    }

    /** Sets the name of the parsed field. */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return "GatewayParamFlowItemVO{"
                + "parseStrategy=" + parseStrategy
                + ", fieldName='" + fieldName + '\''
                + ", pattern='" + pattern + '\''
                + ", matchStrategy=" + matchStrategy
                + '}';
    }
}
