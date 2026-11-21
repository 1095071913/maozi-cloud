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
 * 按匹配模式匹配请求的网关条目基类，持有公共的 pattern 与 matchStrategy 字段。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public abstract class BasePatternItemEntity {

    /** 请求匹配模式。 */
    protected String pattern;
    /** 匹配模式的匹配策略。 */
    protected Integer matchStrategy;

    /** 获取请求匹配模式。 */
    public String getPattern() {
        return pattern;
    }

    /** 设置请求匹配模式。 */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /** 获取匹配模式的匹配策略。 */
    public Integer getMatchStrategy() {
        return matchStrategy;
    }

    /** 设置匹配模式的匹配策略。 */
    public void setMatchStrategy(Integer matchStrategy) {
        this.matchStrategy = matchStrategy;
    }
}
