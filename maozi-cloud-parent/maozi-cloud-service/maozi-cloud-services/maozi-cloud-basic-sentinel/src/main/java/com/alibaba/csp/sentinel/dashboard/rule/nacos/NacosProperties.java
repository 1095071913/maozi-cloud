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
 * Nacos connection properties for rule persistence.
 *
 * @author maozi
 */
@Component
public class NacosProperties {

    /** Nacos server address, defaults to {@code localhost:8848}. */
    @Value("${nacos.server-addr:localhost:8848}")
    private String serverAddr;

    /** Nacos namespace, empty string means the default namespace. */
    @Value("${nacos.namespace:}")
    private String namespace;

    /** Nacos username for authentication, empty when auth is disabled. */
    @Value("${nacos.username:}")
    private String username;

    /** Nacos password for authentication, empty when auth is disabled. */
    @Value("${nacos.password:}")
    private String password;

    /**
     * Group of all sentinel rule configs, always {@link NacosConfigUtil#GROUP_ID}
     * (SENTINEL_GROUP) unless overridden with a non-blank value.
     */
    @Value("${nacos.group:SENTINEL_GROUP}")
    private String group;

    public String getServerAddr() {
        return serverAddr;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /** Returns the configured group, or {@link NacosConfigUtil#GROUP_ID} when it is blank. */
    public String getGroup() {
        // Fall back to SENTINEL_GROUP when the property is absent or blank,
        // so rules are never published to an empty/default group by accident.
        return StringUtil.isBlank(group) ? NacosConfigUtil.GROUP_ID : group;
    }
}
