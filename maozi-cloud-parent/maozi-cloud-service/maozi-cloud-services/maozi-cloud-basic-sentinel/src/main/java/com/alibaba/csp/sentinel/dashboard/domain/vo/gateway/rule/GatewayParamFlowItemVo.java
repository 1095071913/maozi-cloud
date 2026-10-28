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

/**
 * Value Object for add or update gateway flow rule.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class GatewayParamFlowItemVo {

    /** Strategy to parse the parameter from the gateway request (URL param, host, header or cookie). */
    private Integer parseStrategy;

    /** Name of the parsed field, e.g. header name or parameter name, depending on parseStrategy. */
    private String fieldName;

    /** Pattern that the parsed parameter value must match for this item to take effect. */
    private String pattern;

    /** Match strategy applied to the parsed parameter value (exact, prefix or regex). */
    private Integer matchStrategy;

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

    /** Returns the pattern that the parameter value must match. */
    public String getPattern() {
        return pattern;
    }

    /** Sets the pattern that the parameter value must match. */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /** Returns the match strategy applied to the parameter value. */
    public Integer getMatchStrategy() {
        return matchStrategy;
    }

    /** Sets the match strategy applied to the parameter value. */
    public void setMatchStrategy(Integer matchStrategy) {
        this.matchStrategy = matchStrategy;
    }
}
