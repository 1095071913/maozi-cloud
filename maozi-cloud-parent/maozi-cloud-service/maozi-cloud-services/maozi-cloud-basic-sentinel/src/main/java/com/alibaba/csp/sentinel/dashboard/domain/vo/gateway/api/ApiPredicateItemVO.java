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

import com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.BasePatternItemVO;

/**
 * 网关 API 定义的断言项（URL 匹配条件），供新增和更新请求使用。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class ApiPredicateItemVO extends BasePatternItemVO {

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ApiPredicateItemVO{"
                + "pattern='" + pattern + '\''
                + ", matchStrategy=" + matchStrategy
                + '}';
    }
}
