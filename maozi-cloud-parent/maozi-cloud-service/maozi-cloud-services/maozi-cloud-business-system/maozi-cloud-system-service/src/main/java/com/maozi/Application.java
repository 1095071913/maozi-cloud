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
 * 系统模块启动类
 * <p>
 * 继承自 BaseApplicationDB，作为系统服务模块的入口类，
 * 负责初始化并启动 Spring Boot 应用程序。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class Application extends BaseApplicationDB {

	/**
	 * 应用程序主入口方法
	 *
	 * @param args 启动参数，由命令行传入
	 */
	public static void main(String[] args) {
		ApplicationRun(args);
	}

}