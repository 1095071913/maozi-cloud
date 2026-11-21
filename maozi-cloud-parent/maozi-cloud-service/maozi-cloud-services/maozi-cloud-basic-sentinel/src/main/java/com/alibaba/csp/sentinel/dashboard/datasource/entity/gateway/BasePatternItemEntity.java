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

/**
 * Base entity for gateway items that match requests by a pattern, holding the common
 * pattern and matchStrategy fields.
 *
 * @author cdfive
 * @since 1.7.0
 */
public abstract class BasePatternItemEntity {

    /** The pattern to match the request. */
    protected String pattern;
    /** The strategy to match the pattern. */
    protected Integer matchStrategy;

    /** Gets the pattern to match the request. */
    public String getPattern() {
        return pattern;
    }

    /** Sets the pattern to match the request. */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /** Gets the strategy to match the pattern. */
    public Integer getMatchStrategy() {
        return matchStrategy;
    }

    /** Sets the strategy to match the pattern. */
    public void setMatchStrategy(Integer matchStrategy) {
        this.matchStrategy = matchStrategy;
    }
}
