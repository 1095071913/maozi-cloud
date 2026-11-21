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
 * 网关中按模式匹配的条目基类，持有公共的 pattern 与 matchStrategy 字段。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public abstract class BasePatternItemVO {

    /** 匹配模式串。 */
    protected String pattern;

    /**
     * 匹配策略，常量定义在 SentinelGatewayConstants 类中。
     *
     * <ul>
     *     <li>0(URL_MATCH_STRATEGY_EXACT)：精确匹配</li>
     *     <li>1(URL_MATCH_STRATEGY_PREFIX)：前缀匹配</li>
     *     <li>2(URL_MATCH_STRATEGY_REGEX)：正则匹配</li>
     * </ul>
     */
    protected Integer matchStrategy;

    /** 获取匹配模式串。 */
    public String getPattern() {
        return pattern;
    }

    /** 设置匹配模式串。 */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /** 获取匹配策略。 */
    public Integer getMatchStrategy() {
        return matchStrategy;
    }

    /** 设置匹配策略。 */
    public void setMatchStrategy(Integer matchStrategy) {
        this.matchStrategy = matchStrategy;
    }
}
