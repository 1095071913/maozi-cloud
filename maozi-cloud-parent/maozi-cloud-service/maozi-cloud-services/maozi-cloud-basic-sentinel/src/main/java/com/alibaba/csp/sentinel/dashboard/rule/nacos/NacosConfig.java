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
 * 用于规则持久化的 Nacos 配置服务
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class NacosConfig {

    /**
     * 根据配置的连接属性创建共享的 Nacos {@link ConfigService}。
     * 服务器地址总是设置；命名空间仅在非空时设置，
     * 用户名/密码仅在用户名非空时设置
     */
    @Bean
    public ConfigService nacosConfigService(NacosProperties nacosProperties) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacosProperties.getServerAddr());
        // 缓存命名空间与用户名，避免同一方法内重复调用
        String namespace = nacosProperties.getNamespace();
        String username = nacosProperties.getUsername();
        if (StringUtil.isNotBlank(namespace)) {
            properties.setProperty(PropertyKeyConst.NAMESPACE, namespace);
        }
        if (StringUtil.isNotBlank(username)) {
            properties.setProperty(PropertyKeyConst.USERNAME, username);
            properties.setProperty(PropertyKeyConst.PASSWORD, nacosProperties.getPassword());
        }
        return ConfigFactory.createConfigService(properties);
    }
}
