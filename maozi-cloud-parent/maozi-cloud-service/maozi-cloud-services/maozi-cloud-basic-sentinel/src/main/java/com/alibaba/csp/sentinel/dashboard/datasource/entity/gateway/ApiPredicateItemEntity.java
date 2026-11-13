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
 * Entity for {@link ApiPredicateItem}.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class ApiPredicateItemEntity {

    /** The pattern to match the request path. */
    private String pattern;

    /** The strategy to match the pattern. */
    private Integer matchStrategy;

    /** Create an empty item. */
    public ApiPredicateItemEntity() {
    }

    /** Create an item with the given pattern and match strategy. */
    public ApiPredicateItemEntity(String pattern, int matchStrategy) {
        this.pattern = pattern;
        this.matchStrategy = matchStrategy;
    }

    /** Get the pattern to match the request path. */
    public String getPattern() {
        return pattern;
    }

    /** Set the pattern to match the request path. */
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

    /** Whether the given object has the same pattern and match strategy. */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ApiPredicateItemEntity that = (ApiPredicateItemEntity) o;
        return Objects.equals(pattern, that.pattern) &&
                Objects.equals(matchStrategy, that.matchStrategy);
    }

    /** Get the hash code from the pattern and match strategy. */
    @Override
    public int hashCode() {
        return Objects.hash(pattern, matchStrategy);
    }

    /** Build a string with all fields of this item. */
    @Override
    public String toString() {
        return "ApiPredicateItemEntity{" +
                "pattern='" + pattern + '\'' +
                ", matchStrategy=" + matchStrategy +
                '}';
    }
}
