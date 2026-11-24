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
package com.alibaba.csp.sentinel.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 控制台认证配置属性，绑定 "auth" 前缀。
 * @author maozi
 * @date 2026/11/22
 */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 是否开启登录认证，默认 true。 */
    private boolean enabled = true;

    /** 是否开启登录认证。 */
    public boolean isEnabled() {
        return enabled;
    }

    /** 设置是否开启登录认证。 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
