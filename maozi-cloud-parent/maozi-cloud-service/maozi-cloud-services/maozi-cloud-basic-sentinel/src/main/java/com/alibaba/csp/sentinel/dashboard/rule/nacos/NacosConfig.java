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
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Nacos config service for rule persistence.
 *
 * @author maozi
 */
@Configuration
public class NacosConfig {

    /**
     * Create the shared Nacos {@link ConfigService} from the configured connection
     * properties. The server address is always set; namespace is only set when
     * non-blank, and username/password are only set when the username is non-blank.
     */
    @Bean
    public ConfigService nacosConfigService(NacosProperties nacosProperties) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacosProperties.getServerAddr());
        if (StringUtil.isNotBlank(nacosProperties.getNamespace())) {
            properties.setProperty(PropertyKeyConst.NAMESPACE, nacosProperties.getNamespace());
        }
        if (StringUtil.isNotBlank(nacosProperties.getUsername())) {
            properties.setProperty(PropertyKeyConst.USERNAME, nacosProperties.getUsername());
            properties.setProperty(PropertyKeyConst.PASSWORD, nacosProperties.getPassword());
        }
        return ConfigFactory.createConfigService(properties);
    }
}
