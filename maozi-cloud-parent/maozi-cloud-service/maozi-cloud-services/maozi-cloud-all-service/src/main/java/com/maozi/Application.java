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
 * 单体全量服务启动入口
 * <p>
 * 继承 {@link BaseApplicationDB}，将 OAuth 认证、系统管理等业务模块聚合为
 * 单一可执行 JAR，通过单体部署插件（maozi-cloud-service-monomer）在单进程内
 * 完成本地 Bean 装配，默认以端口 1000 对外提供服务（可经环境变量 APPLICATION_PORT
 * 或 application.port 配置覆盖）。
 * </p>
 *
 * @author maozi
 */
public class Application extends BaseApplicationDB {

	/**
	 * 应用程序主入口方法。
	 *
	 * @param args 启动参数
	 */
    public static void main(String[] args) {
        ApplicationRun(args);
    }

}