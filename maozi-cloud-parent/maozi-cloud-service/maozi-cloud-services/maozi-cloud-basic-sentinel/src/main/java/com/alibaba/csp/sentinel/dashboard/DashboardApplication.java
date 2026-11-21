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
package com.alibaba.csp.sentinel.dashboard;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.init.InitExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Sentinel 控制台启动类。
 *
 * @author Carpenter Lee
 * @date 2026/11/22
 */
@SpringBootApplication
public class DashboardApplication {

    /**
     * 程序入口：异步触发 Sentinel 初始化后启动 Spring 应用。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        triggerSentinelInit();
        SpringApplication.run(DashboardApplication.class, args);
    }

    /**
     * 在独立线程池中执行 Sentinel {@link InitExecutor} 初始化函数，避免阻塞应用启动。
     */
    private static void triggerSentinelInit() {
        ExecutorService initExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new NamedThreadFactory("sentinel-dashboard-init", true));
        initExecutor.execute(InitExecutor::doInit);
        initExecutor.shutdown();
    }
}
