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
 * {@link GatewayParamFlowItem} 对应的网关参数限流条目实体。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class GatewayParamFlowItemEntity extends BasePatternItemEntity {

    /** 从请求中解析参数值的策略。 */
    private Integer parseStrategy;

    /** 待解析的 Header 或参数名，由 parseStrategy 决定。 */
    private String fieldName;

    /** 获取从请求中解析参数值的策略。 */
    public Integer getParseStrategy() {
        return parseStrategy;
    }

    /** 设置从请求中解析参数值的策略。 */
    public void setParseStrategy(Integer parseStrategy) {
        this.parseStrategy = parseStrategy;
    }

    /** 获取待解析的 Header 或参数名。 */
    public String getFieldName() {
        return fieldName;
    }

    /** 设置待解析的 Header 或参数名。 */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /** 判断指定对象是否与本条目全部字段相同。 */
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

    /** 以全部字段计算哈希码。 */
    @Override
    public int hashCode() {
        return Objects.hash(parseStrategy, fieldName, pattern, matchStrategy);
    }

    /** 拼接全部字段构建字符串。 */
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
