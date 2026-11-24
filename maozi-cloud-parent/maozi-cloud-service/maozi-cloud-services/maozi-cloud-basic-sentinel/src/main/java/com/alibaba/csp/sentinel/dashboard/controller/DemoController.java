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
package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 演示控制器：提供用于测试 Sentinel 资源、调用链路与流量生产的示例端点。
 * @author maozi
 * @date 2026/11/22
 */
@Controller
@RequestMapping(value = "/demo", produces = MediaType.APPLICATION_JSON_VALUE)
public class DemoController {

    /** 日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    /** 每秒毫秒数，用于将演示时长换算为流逝时间判断。 */
    private static final long MILLIS_PER_SECOND = 1000L;

    /** 慢调用模式下每次 entry 后的休眠时间（毫秒）。 */
    private static final long SLOW_CALL_SLEEP_MS = 3000L;

    /** 每次 loop/slow 请求启动的演示流量线程数，同时作为线程池的核心与最大线程数。 */
    private static final int LOOP_THREAD_COUNT = 10;

    /** 演示流量线程池空闲线程的存活时间（秒）。 */
    private static final long DEMO_KEEP_ALIVE_SECONDS = 60L;

    /** 演示流量线程池的任务队列容量。 */
    private static final int DEMO_QUEUE_CAPACITY = 100;

    /** 演示流量循环每轮之间随机暂停时长的上限（毫秒）。 */
    private static final int LOOP_PAUSE_MAX_MILLIS = 200;

    /** 演示流量线程池，使用带业务含义名称的 ThreadPoolExecutor。 */
    private static final ExecutorService DEMO_EXECUTOR = new ThreadPoolExecutor(LOOP_THREAD_COUNT, LOOP_THREAD_COUNT,
            DEMO_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<>(DEMO_QUEUE_CAPACITY),
            new NamedThreadFactory("demo-traffic", true), new ThreadPoolExecutor.CallerRunsPolicy());

    /** 返回 "index" 视图名。 */
    @GetMapping("/greeting")
    public String greeting() {
        return "index";
    }

    /** 创建四级嵌套调用链（head1 -> head2 -> head3 -> head4），每级均有 Sentinel entry。 */
    @GetMapping("/link")
    @ResponseBody
    public String link() throws BlockException {

        Entry entry = SphU.entry("head1", EntryType.IN);

        Entry entry1 = SphU.entry("head2", EntryType.IN);
        Entry entry2 = SphU.entry("head3", EntryType.IN);
        Entry entry3 = SphU.entry("head4", EntryType.IN);

        entry3.exit();
        entry2.exit();
        entry1.exit();
        entry.exit();
        return "successfully create a call link";
    }

    /** 启动 10 个线程，在指定秒数内持续进入指定资源。 */
    @GetMapping("/loop")
    @ResponseBody
    public String loop(String name, int time) throws BlockException {
        for (int i = 0; i < LOOP_THREAD_COUNT; i++) {
            DEMO_EXECUTOR.execute(new RunTask(name, time, false));
        }
        return "successfully create a loop thread";
    }

    /** 启动 10 个线程，在指定秒数内持续进入指定资源，每次 entry 后休眠 3 秒模拟慢调用。 */
    @GetMapping("/slow")
    @ResponseBody
    public String slow(String name, int time) throws BlockException {
        for (int i = 0; i < LOOP_THREAD_COUNT; i++) {
            DEMO_EXECUTOR.execute(new RunTask(name, time, true));
        }
        return "successfully create a loop thread";
    }

    /**
     * 在独立 context 中持续进入 Sentinel 资源直至时长耗尽的任务。
     *
     * @author maozi
     * @date 2026/11/22
     */
    private static final class RunTask implements Runnable {
        /** 任务持续运行的秒数。 */
        private final int time;
        /** 待进入的 Sentinel 资源名。 */
        private final String name;
        /** 是否在每次 entry 后休眠 3 秒以模拟慢调用。 */
        private final boolean slow;

        /** 构造在指定秒数内进入指定资源的任务，慢调用模式下每次 entry 休眠 3 秒。 */
        private RunTask(String name, int time, boolean slow) {
            super();
            this.time = time;
            this.name = name;
            this.slow = slow;
        }

        /**
         * 进入以起始时间戳命名的 context，循环进入资源（慢调用模式下休眠 3 秒，
         * 每轮之间最多暂停 200ms）直至时长耗尽。
         */
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            ContextUtil.enter(String.valueOf(startTime));
            boolean stop = false;
            while (!stop) {
                long now = System.currentTimeMillis();
                if (now - startTime > time * MILLIS_PER_SECOND) {
                    stop = true;
                }
                Entry entry = null;
                try {
                    entry = SphU.entry(name);
                    if (slow) {
                        TimeUnit.MILLISECONDS.sleep(SLOW_CALL_SLEEP_MS);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stop = true;
                } catch (Exception e) {
                    // 进入演示资源被限流或系统异常：保留堆栈便于观察
                    logger.warn("Demo task entry error, resource: {}", name, e);
                } finally {
                    if (entry != null) {
                        entry.exit();
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(LOOP_PAUSE_MAX_MILLIS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stop = true;
                    logger.warn("Demo loop task interrupted", e);
                }
            }
            ContextUtil.exit();
        }
    }

}
