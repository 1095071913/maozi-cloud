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
package com.alibaba.csp.sentinel.dashboard.rule;

/**
 * 将应用规则发布到远端规则仓库的接口
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public interface DynamicRulePublisher<T> {

    /**
     * 将规则发布到指定应用的远端规则配置中心
     *
     * @param app 应用名
     * @param rules 待推送的规则列表
     * @throws Exception 出现错误时抛出
     */
    void publish(String app, T rules) throws Exception;
}
