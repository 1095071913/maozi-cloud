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

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;

import java.util.Objects;

/**
 * Entity for {@link GatewayParamFlowItem}.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class GatewayParamFlowItemEntity {

    /** The strategy of parsing the parameter value from the request. */
    private Integer parseStrategy;

    /** The name of the header or parameter to parse, depending on parseStrategy. */
    private String fieldName;

    /** The pattern to match the parsed value. */
    private String pattern;

    /** The strategy to match the pattern. */
    private Integer matchStrategy;

    /** Get the strategy of parsing the parameter value from the request. */
    public Integer getParseStrategy() {
        return parseStrategy;
    }

    /** Set the strategy of parsing the parameter value from the request. */
    public void setParseStrategy(Integer parseStrategy) {
        this.parseStrategy = parseStrategy;
    }

    /** Get the name of the header or parameter to parse. */
    public String getFieldName() {
        return fieldName;
    }

    /** Set the name of the header or parameter to parse. */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /** Get the pattern to match the parsed value. */
    public String getPattern() {
        return pattern;
    }

    /** Set the pattern to match the parsed value. */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /** Get the strategy to match the pattern. */
    public Integer getMatchStrategy() {
        return matchStrategy;
    }

    /** Set the strategy to match the pattern. */
    public void setMatchStrategy(Integer matchStrategy) {
        this.matchStrategy = matchStrategy;
    }

    /** Whether the given object has the same fields as this item. */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        GatewayParamFlowItemEntity that = (GatewayParamFlowItemEntity) o;
        return Objects.equals(parseStrategy, that.parseStrategy) &&
                Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(pattern, that.pattern) &&
                Objects.equals(matchStrategy, that.matchStrategy);
    }

    /** Get the hash code from all fields of this item. */
    @Override
    public int hashCode() {
        return Objects.hash(parseStrategy, fieldName, pattern, matchStrategy);
    }

    /** Build a string with all fields of this item. */
    @Override
    public String toString() {
        return "GatewayParamFlowItemEntity{" +
                "parseStrategy=" + parseStrategy +
                ", fieldName='" + fieldName + '\'' +
                ", pattern='" + pattern + '\'' +
                ", matchStrategy=" + matchStrategy +
                '}';
    }
}
