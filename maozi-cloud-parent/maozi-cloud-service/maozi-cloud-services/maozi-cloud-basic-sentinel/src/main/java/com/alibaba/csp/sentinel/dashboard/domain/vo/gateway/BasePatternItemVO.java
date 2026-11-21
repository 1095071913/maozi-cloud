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
package com.alibaba.csp.sentinel.dashboard.domain.vo.gateway;

/**
 * Base value object for gateway items that match by a pattern, holding the common
 * pattern and matchStrategy fields.
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public abstract class BasePatternItemVO {

    /** The pattern for matching. */
    protected String pattern;

    /**
     * The matching strategy. Constants are defined in class SentinelGatewayConstants.
     *
     * <ul>
     *     <li>0(URL_MATCH_STRATEGY_EXACT): exact match mode</li>
     *     <li>1(URL_MATCH_STRATEGY_PREFIX): prefix match mode</li>
     *     <li>2(URL_MATCH_STRATEGY_REGEX): regex match mode</li>
     * </ul>
     */
    protected Integer matchStrategy;

    /** Returns the pattern for matching. */
    public String getPattern() {
        return pattern;
    }

    /** Sets the pattern for matching. */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /** Returns the matching strategy. */
    public Integer getMatchStrategy() {
        return matchStrategy;
    }

    /** Sets the matching strategy. */
    public void setMatchStrategy(Integer matchStrategy) {
        this.matchStrategy = matchStrategy;
    }
}
