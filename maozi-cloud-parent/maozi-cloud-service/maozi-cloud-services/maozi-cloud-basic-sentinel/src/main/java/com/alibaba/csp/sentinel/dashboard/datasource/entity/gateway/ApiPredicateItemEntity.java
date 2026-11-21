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

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;

import java.util.Objects;

/**
 * {@link ApiPredicateItem} 对应的 API 断言条目实体。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class ApiPredicateItemEntity extends BasePatternItemEntity {

    /** 构造空条目。 */
    public ApiPredicateItemEntity() {
    }

    /** 以指定匹配模式与匹配策略构造条目。 */
    public ApiPredicateItemEntity(String pattern, int matchStrategy) {
        this.pattern = pattern;
        this.matchStrategy = matchStrategy;
    }

    /** 判断指定对象是否与本条目的匹配模式和匹配策略相同。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ApiPredicateItemEntity that = (ApiPredicateItemEntity) o;
        return Objects.equals(pattern, that.pattern) &&
                Objects.equals(matchStrategy, that.matchStrategy);
    }

    /** 以匹配模式与匹配策略计算哈希码。 */
    @Override
    public int hashCode() {
        return Objects.hash(pattern, matchStrategy);
    }

    /** 拼接全部字段构建字符串。 */
    @Override
    public String toString() {
        return "ApiPredicateItemEntity{" +
                "pattern='" + pattern + '\'' +
                ", matchStrategy=" + matchStrategy +
                '}';
    }
}
