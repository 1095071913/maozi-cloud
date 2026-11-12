/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.maozi.monitor.config;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Admin Server 配置类
 * <p>
 * 启用 Spring Boot Admin Server 监控中心功能，
 * 自动发现并监控注册到 Nacos 的所有微服务实例。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
@EnableAdminServer
public class Config {}
