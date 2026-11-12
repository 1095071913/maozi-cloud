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

package com.maozi;

/**
 * 监控服务启动类。
 * <p>
 * 继承 BaseApplication 以复用公共的应用启动逻辑（如 Nacos 配置加载、日志初始化等），
 * 作为 Spring Boot Admin Server 的入口点，提供微服务监控仪表盘功能，
 * 可查看各服务的健康状态、JVM 指标、线程信息、日志级别等。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class Application extends BaseApplication {

    /**
     * 应用程序主入口方法。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        applicationRun(args);
    }

}
