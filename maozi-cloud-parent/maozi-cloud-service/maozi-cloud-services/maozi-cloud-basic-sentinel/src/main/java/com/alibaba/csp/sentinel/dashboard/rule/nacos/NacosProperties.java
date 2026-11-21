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
package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 用于规则持久化的 Nacos 连接配置
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class NacosProperties {

    /** Nacos 服务器地址，默认 {@code localhost:8848} */
    @Value("${nacos.server-addr:localhost:8848}")
    private String serverAddr;

    /** Nacos 命名空间，空字符串表示默认命名空间 */
    @Value("${nacos.namespace:}")
    private String namespace;

    /** 认证用的 Nacos 用户名，关闭认证时为空 */
    @Value("${nacos.username:}")
    private String username;

    /** 认证用的 Nacos 密码，关闭认证时为空 */
    @Value("${nacos.password:}")
    private String password;

    /**
     * 全部 Sentinel 规则配置所在分组，未用非空值覆盖时
     * 恒为 {@link NacosConfigUtil#GROUP_ID}（SENTINEL_GROUP）
     */
    @Value("${nacos.group:SENTINEL_GROUP}")
    private String group;

    /** 返回 Nacos 服务器地址 */
    public String getServerAddr() {
        return serverAddr;
    }

    /** 返回 Nacos 命名空间，空字符串表示默认命名空间 */
    public String getNamespace() {
        return namespace;
    }

    /** 返回 Nacos 用户名，关闭认证时为空 */
    public String getUsername() {
        return username;
    }

    /** 返回 Nacos 密码，关闭认证时为空 */
    public String getPassword() {
        return password;
    }

    /** 返回配置的分组，为空时返回 {@link NacosConfigUtil#GROUP_ID} */
    public String getGroup() {
        // 配置缺失或为空时回退到 SENTINEL_GROUP，避免规则被意外发布到空分组/默认分组
        return StringUtil.isBlank(group) ? NacosConfigUtil.GROUP_ID : group;
    }
}
