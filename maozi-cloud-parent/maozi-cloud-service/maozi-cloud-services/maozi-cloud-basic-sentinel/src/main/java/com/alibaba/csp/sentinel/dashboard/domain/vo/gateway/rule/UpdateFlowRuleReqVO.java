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
 * 修改网关流控规则请求 VO
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class UpdateFlowRuleReqVO extends BaseFlowRuleReqVO {

    /** 待修改的网关流控规则 ID */
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "UpdateFlowRuleReqVO{"
                + "id=" + id
                + ", app='" + app + '\''
                + ", grade=" + grade
                + ", count=" + count
                + ", interval=" + interval
                + ", intervalUnit=" + intervalUnit
                + ", controlBehavior=" + controlBehavior
                + ", burst=" + burst
                + ", maxQueueingTimeoutMs=" + maxQueueingTimeoutMs
                + ", paramItem=" + paramItem
                + '}';
    }
}
