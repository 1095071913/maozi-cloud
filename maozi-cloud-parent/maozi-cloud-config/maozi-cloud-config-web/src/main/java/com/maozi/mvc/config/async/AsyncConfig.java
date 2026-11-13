
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

package com.maozi.mvc.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * <p>
 * 配置应用级异步任务线程池，用于 {@code @Async} 注解标注的异步方法执行，
 * 拒绝策略为 CallerRunsPolicy（由调用线程执行），确保任务不丢失。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class AsyncConfig {

	/** 核心线程数 */
	private static final int CORE_POOL_SIZE = 16;

	/** 最大线程数 */
	private static final int MAX_POOL_SIZE = 256;

	/** 超出核心线程数的空闲线程存活时间（秒），超过后回收 */
	private static final int KEEP_ALIVE_TIME = 10;

	/** 缓冲队列容量 */
	private static final int QUEUE_CAPACITY = 1024;

	/** 应用关闭时等待任务完成的最长时间（秒），超时后不再等待，仅记录警告并继续关闭流程 */
	private static final int AWAIT_TERMINATION = 60;

	/** 应用关闭时是否等待所有异步任务执行完成后再销毁线程池 */
	private static final Boolean WAIT_FOR_TASKS_TO_COMPLETE_ON_SHUTDOWN = true;

	/** 线程名前缀 */
	private static final String THREAD_NAME_PREFIX = "Async-Service-";

	/**
	 * 创建异步任务线程池
	 * <p>
	 * Bean 名沿用 {@code applicationTaskExecutor}，替代 Spring Boot 自动配置的默认执行器。
	 * </p>
	 *
	 * @return 配置好的线程池任务执行器
	 */
	@Bean("applicationTaskExecutor")
	public ThreadPoolTaskExecutor asyncTaskExecutor() {

		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

		// 设置核心线程数：线程池中长期保持的线程数量
		taskExecutor.setCorePoolSize(CORE_POOL_SIZE);

		// 设置最大线程数：在队列满后可扩展的最大线程数量
		taskExecutor.setMaxPoolSize(MAX_POOL_SIZE);

		// 设置空闲线程存活时间：超出核心线程数的空闲线程超过此时间后被回收
		taskExecutor.setKeepAliveSeconds(KEEP_ALIVE_TIME);

		// 设置缓冲队列容量：核心线程满后，任务先放入队列等待
		taskExecutor.setQueueCapacity(QUEUE_CAPACITY);

		// 设置线程名前缀，便于日志排查和线程识别
		taskExecutor.setThreadNamePrefix(THREAD_NAME_PREFIX);

		// 应用关闭时等待所有异步任务完成后再销毁线程池
		taskExecutor.setWaitForTasksToCompleteOnShutdown(WAIT_FOR_TASKS_TO_COMPLETE_ON_SHUTDOWN);

		// 等待任务完成的最长时间（秒），超时后仅记录警告，不强制中断任务
		taskExecutor.setAwaitTerminationSeconds(AWAIT_TERMINATION);

		// 设置拒绝策略：队列和最大线程数都满后，由调用线程自己执行任务，避免任务丢失
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		// 初始化线程池
		taskExecutor.initialize();

		return taskExecutor;

	}

}
