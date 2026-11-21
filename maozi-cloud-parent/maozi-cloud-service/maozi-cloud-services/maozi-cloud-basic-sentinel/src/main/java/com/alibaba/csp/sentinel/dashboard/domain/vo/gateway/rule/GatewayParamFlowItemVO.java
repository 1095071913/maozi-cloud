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
 * 网关流控规则的热点参数项，供新增/修改规则请求使用
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class GatewayParamFlowItemVO extends BasePatternItemVO {

    /** 从网关请求中提取参数的策略（URL 参数、Host、Header 或 Cookie） */
    private Integer parseStrategy;

    /** 提取字段名称，如 Header 名或参数名，取决于 parseStrategy */
    private String fieldName;

    public Integer getParseStrategy() {
        return parseStrategy;
    }

    public void setParseStrategy(Integer parseStrategy) {
        this.parseStrategy = parseStrategy;
    }

    public String getFieldName() {
        return fieldName;
    }

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
