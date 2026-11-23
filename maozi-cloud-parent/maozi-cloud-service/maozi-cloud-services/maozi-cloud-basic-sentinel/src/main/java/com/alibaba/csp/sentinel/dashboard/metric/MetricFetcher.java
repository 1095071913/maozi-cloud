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
package com.alibaba.csp.sentinel.dashboard.metric;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 从各机器拉取监控指标数据
 *
 * @author leyou
 * @date 2026/11/22
 */
@Component
public class MetricFetcher {

    /** 客户端在请求时间窗口内无指标数据时返回的响应体前缀 */
    public static final String NO_METRICS = "No metrics";
    /** 表示拉取成功的 HTTP 状态码 */
    private static final int HTTP_OK = 200;
    /** 应用无有效最近拉取时间时最多向前回溯的时间窗口（毫秒） */
    private static final long MAX_LAST_FETCH_INTERVAL_MS = 1000 * 15;
    /** 每次拉取的时间窗口长度（秒） */
    private static final long FETCH_INTERVAL_SECOND = 6;
    /** 拉取请求的连接超时时间（毫秒） */
    private static final int HTTP_CONNECT_TIMEOUT_MS = 3000;
    /** 拉取请求的读超时时间（毫秒） */
    private static final int HTTP_SO_TIMEOUT_MS = 3000;
    /** 异步 HTTP 客户端的最大总连接数 */
    private static final int MAX_CONN_TOTAL = 4000;
    /** 异步 HTTP 客户端的每路由最大连接数 */
    private static final int MAX_CONN_PER_ROUTE = 1000;
    /** 定时拉取任务的初始延迟（秒） */
    private static final long FETCH_INITIAL_DELAY_SECONDS = 10;
    /** 指标聚合 Map 的初始容量 */
    private static final int METRIC_MAP_INITIAL_CAPACITY = 16;
    /** 拉取与 IO 线程池大小的 CPU 核数倍数 */
    private static final int CPU_CORE_MULTIPLIER = 2;
    /** 一秒对应的毫秒数 */
    private static final long ONE_SECOND_MS = 1000L;
    /** 拉取窗口结束时间距当前时间小于该秒数时跳过本轮 */
    private static final int NEAR_SKIP_WINDOW_SECONDS = 2;
    /** 单次拉取等待 HTTP 客户端的最大秒数 */
    private static final int MAX_WAIT_SECONDS = 5;
    /** 拉取线程池的任务队列容量 */
    private static final int FETCH_QUEUE_SIZE = 2048;
    /** 响应体未声明字符集时使用的默认字符集 */
    private static final Charset DEFAULT_CHARSET = Charset.forName(SentinelConfig.charset());
    /** Sentinel 客户端上指标查询命令的路径 */
    private static final String METRIC_URL_PATH = "metric";
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(MetricFetcher.class);
    /** 定时拉取全部应用任务的执行周期（秒） */
    private final long intervalSecond = 1;

    /** 各应用最近一次拉取窗口的结束时间（毫秒），用于计算下一次拉取窗口 */
    private Map<String, AtomicLong> appLastFetchTime = new ConcurrentHashMap<>();

    /** 指标仓储，用于持久化拉取到的指标实体 */
    private final MetricsRepository<MetricEntity> metricStore;
    /** 应用与机器注册信息，用于枚举拉取目标 */
    private final AppManagement appManagement;

    /** 共享的异步 HTTP 客户端，用于从 Sentinel 客户端拉取指标 */
    private CloseableHttpAsyncClient httpclient;

    /** 定时调度器，周期性触发所有应用的指标拉取 */
    @SuppressWarnings("PMD.ThreadPoolCreationRule")
    private ScheduledExecutorService fetchScheduleService = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("sentinel-dashboard-metrics-fetch-task", true),
            new ThreadPoolExecutor.AbortPolicy());
    /** 执行各应用指标拉取分发任务的线程池 */
    private ExecutorService fetchService;
    /** 执行实际拉取任务（HTTP 往返）的线程池 */
    private ExecutorService fetchWorker;

    /**
     * 创建拉取线程池与共享异步 HTTP 客户端，并启动定时拉取调度
     */
    public MetricFetcher(MetricsRepository<MetricEntity> metricStore, AppManagement appManagement) {
        this.metricStore = metricStore;
        this.appManagement = appManagement;
        int cores = Runtime.getRuntime().availableProcessors() * CPU_CORE_MULTIPLIER;
        long keepAliveTime = 0;
        int queueSize = FETCH_QUEUE_SIZE;
        RejectedExecutionHandler handler = new DiscardPolicy();
        fetchService = new ThreadPoolExecutor(cores, cores,
            keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
            new NamedThreadFactory("sentinel-dashboard-metrics-fetchService", true), handler);
        fetchWorker = new ThreadPoolExecutor(cores, cores,
            keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
            new NamedThreadFactory("sentinel-dashboard-metrics-fetchWorker", true), handler);
        IOReactorConfig ioConfig = IOReactorConfig.custom()
            .setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS)
            .setSoTimeout(HTTP_SO_TIMEOUT_MS)
            // 复用已计算的核数，避免再次查询运行时
            .setIoThreadCount(cores)
            .build();

        httpclient = HttpAsyncClients.custom()
            .setRedirectStrategy(new DefaultRedirectStrategy() {
                /** 拉取指标时不跟随重定向 */
                @Override
                protected boolean isRedirectable(final String method) {
                    return false;
                }
            }).setMaxConnTotal(MAX_CONN_TOTAL)
            .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
            .setDefaultIOReactorConfig(ioConfig)
            .build();
        httpclient.start();
        start();
    }

    /** 延迟 10 秒后按 {@link #intervalSecond} 秒的周期调度 {@link #fetchAllApp} */
    private void start() {
        fetchScheduleService.scheduleAtFixedRate(() -> {
            try {
                fetchAllApp();
            } catch (Exception e) {
                logger.info("fetchAllApp error:", e);
            }
        }, FETCH_INITIAL_DELAY_SECONDS, intervalSecond, TimeUnit.SECONDS);
    }

    /** 为指标批量写入创建/更新时间并持久化，空数据直接忽略 */
    private void writeMetric(Map<String, MetricEntity> map) {
        if (map.isEmpty()) {
            return;
        }
        Date date = new Date();
        Collection<MetricEntity> metrics = map.values();
        for (MetricEntity entity : metrics) {
            entity.setGmtCreate(date);
            entity.setGmtModified(date);
        }
        metricStore.saveAll(metrics);
    }

    /**
     * 遍历每个应用，拉取该应用下所有机器的指标数据
     */
    private void fetchAllApp() {
        List<String> apps = appManagement.getAppNames();
        if (apps == null) {
            return;
        }
        for (final String app : apps) {
            fetchService.submit(() -> {
                try {
                    doFetchAppMetric(app);
                } catch (Exception e) {
                    logger.error("fetchAppMetric error", e);
                }
            });
        }
    }

    /**
     * 拉取 [startTime, endTime] 区间（双边均含）的指标数据
     */
    private void fetchOnce(String app, long startTime, long endTime, int maxWaitSeconds) {
        if (maxWaitSeconds <= 0) {
            throw new IllegalArgumentException("maxWaitSeconds must > 0, but " + maxWaitSeconds);
        }
        AppInfo appInfo = appManagement.getDetailApp(app);
        // 应用失活时自动移除
        if (appInfo.isDead()) {
            logger.info("Dead app removed: {}", app);
            appManagement.removeApp(app);
            return;
        }
        Set<MachineInfo> machines = appInfo.getMachines();
        int machineCount = machines.size();
        logger.debug("enter fetchOnce({}), machines.size()={}, time intervalMs [{}, {}]",
            app, machineCount, startTime, endTime);
        if (machines.isEmpty()) {
            return;
        }
        final String msg = "fetch";
        AtomicLong unhealthy = new AtomicLong();
        final AtomicLong success = new AtomicLong();
        final AtomicLong fail = new AtomicLong();

        // 映射：app_resource_timeSecond -> metric
        final Map<String, MetricEntity> metricMap = new ConcurrentHashMap<>(METRIC_MAP_INITIAL_CAPACITY);
        final CountDownLatch latch = new CountDownLatch(machineCount);
        for (final MachineInfo machine : machines) {
            // 健康状态失效（失联）的机器自动移除
            if (!machine.isHealthy()) {
                latch.countDown();
                unhealthy.incrementAndGet();
                String machineIp = machine.getIp();
                int machinePort = machine.getPort();
                // 复用方法开头获取的应用信息，避免同一方法内重复调用
                appInfo.removeMachine(machineIp, machinePort);
                logger.info("Unhealthy machine removed: {}:{} of {}", machineIp, machinePort, app);
                continue;
            }
            asyncFetchMachineMetric(machine, startTime, endTime, metricMap, latch, msg, success, fail);
        }
        try {
            latch.await(maxWaitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("{} metric, wait http client error:", msg, e);
        } catch (Exception e) {
            logger.info("{} metric, wait http client error:", msg, e);
        }
        writeMetric(metricMap);
    }

    /**
     * 向指定机器异步发送指标拉取请求，回调中解析响应写入共享指标 Map 并保证计数器递减
     */
    private void asyncFetchMachineMetric(final MachineInfo machine, long startTime, long endTime,
            final Map<String, MetricEntity> metricMap, final CountDownLatch latch, final String msg,
            final AtomicLong success, final AtomicLong fail) {
        final String url = "http://" + machine.getIp() + ":" + machine.getPort() + "/" + METRIC_URL_PATH
            + "?startTime=" + startTime + "&endTime=" + endTime + "&refetch=" + false;
        final HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        httpclient.execute(httpGet, new FutureCallback<HttpResponse>() {
            /** 将成功响应交给 handleResponse 处理并递减计数器 */
            @Override
            public void completed(final HttpResponse response) {
                try {
                    handleResponse(response, machine, metricMap);
                    success.incrementAndGet();
                } catch (Exception e) {
                    logger.error("{} metric {} error:", msg, url, e);
                } finally {
                    latch.countDown();
                }
            }

            /** 递减计数器，中止请求并按异常类型记录失败日志 */
            @Override
            public void failed(final Exception ex) {
                latch.countDown();
                fail.incrementAndGet();
                httpGet.abort();
                if (ex instanceof SocketTimeoutException) {
                    logger.error("Failed to fetch metric from <{}>: socket timeout", url, ex);
                } else if (ex instanceof ConnectException) {
                    logger.error("Failed to fetch metric from <{}> (ConnectionException: {})",
                        url, ex.getMessage(), ex);
                } else {
                    logger.error("{} metric {} error", msg, url, ex);
                }
            }

            /** 递减计数器，中止请求并计为失败 */
            @Override
            public void cancelled() {
                latch.countDown();
                fail.incrementAndGet();
                httpGet.abort();
            }
        });
    }

    /**
     * 根据记录的最近拉取时间计算下一次拉取窗口（最多回溯
     * {@link #MAX_LAST_FETCH_INTERVAL_MS}），并提交到 {@link #fetchWorker} 执行；
     * 窗口结束时间距今不足 2 秒时跳过本轮，留待下次拉取
     */
    private void doFetchAppMetric(final String app) {
        long now = System.currentTimeMillis();
        long lastFetchMs = now - MAX_LAST_FETCH_INTERVAL_MS;
        if (appLastFetchTime.containsKey(app)) {
            lastFetchMs = Math.max(lastFetchMs, appLastFetchTime.get(app).get() + ONE_SECOND_MS);
        }
        // 截断毫秒
        lastFetchMs = lastFetchMs / ONE_SECOND_MS * ONE_SECOND_MS;
        long endTime = lastFetchMs + FETCH_INTERVAL_SECOND * ONE_SECOND_MS;
        if (endTime > now - ONE_SECOND_MS * NEAR_SKIP_WINDOW_SECONDS) {
            // 距当前时间过近，指标尚未稳定
            return;
        }
        // 提前更新最近拉取时间，避免重复拉取同一窗口
        appLastFetchTime.computeIfAbsent(app, a -> new AtomicLong()).set(endTime);
        final long finalLastFetchMs = lastFetchMs;
        final long finalEndTime = endTime;
        try {
            // 异步执行真正的拉取
            fetchWorker.submit(() -> {
                try {
                    fetchOnce(app, finalLastFetchMs, finalEndTime, MAX_WAIT_SECONDS);
                } catch (Exception e) {
                    logger.info("fetchOnce({}) error", app, e);
                }
            });
        } catch (Exception e) {
            logger.info("submit fetchOnce({}) fail, intervalMs [{}, {}]", app, lastFetchMs, endTime, e);
        }
    }

    /**
     * 处理一次拉取响应：忽略非 200 响应，按 Content-Type 字符集解码
     * （未声明时回退到 {@link #DEFAULT_CHARSET}），跳过空响应体或
     * {@link #NO_METRICS} 前缀响应体，其余行交由 {@link #handleBody} 解析
     */
    private void handleResponse(final HttpResponse response, MachineInfo machine,
                                Map<String, MetricEntity> metricMap) throws Exception {
        int code = response.getStatusLine().getStatusCode();
        if (code != HTTP_OK) {
            return;
        }
        Charset charset = null;
        try {
            String contentTypeStr = response.getFirstHeader("Content-type").getValue();
            if (StringUtil.isNotEmpty(contentTypeStr)) {
                ContentType contentType = ContentType.parse(contentTypeStr);
                charset = contentType.getCharset();
            }
        } catch (Exception ignore) {
            // Content-Type 解析失败时下方回退到默认字符集，debug 日志仅用于排查
            logger.debug("Parse Content-Type charset failed, fallback to {}", DEFAULT_CHARSET, ignore);
        }
        String body = EntityUtils.toString(response.getEntity(), charset != null ? charset : DEFAULT_CHARSET);
        if (StringUtil.isEmpty(body) || body.startsWith(NO_METRICS)) {
            return;
        }
        String[] lines = body.split("\n");
        handleBody(lines, machine, metricMap);
    }

    /**
     * 将每行指标解析为 {@link MetricNode}，跳过排除资源后按
     * app__resource__timeSecond 为 key 聚合，合并该应用所有机器的
     * 指标数据（忽略 ip 与端口）
     */
    private void handleBody(String[] lines, MachineInfo machine, Map<String, MetricEntity> map) {
        if (lines.length < 1) {
            return;
        }

        for (String line : lines) {
            try {
                MetricNode node = MetricNode.fromThinString(line);
                String resource = node.getResource();
                if (shouldFilterOut(resource)) {
                    continue;
                }
                /*
                 * 按 app_resource_timeSecond 聚合指标，忽略 ip 与端口
                 */
                // 缓存应用名与秒级时间戳，聚合 key 与实体初始化共用，避免同一方法内重复调用
                String app = machine.getApp();
                long timestamp = node.getTimestamp();
                String key = buildMetricKey(app, resource, timestamp);

                MetricEntity metricEntity = map.computeIfAbsent(key, s -> {
                    MetricEntity initMetricEntity = new MetricEntity();
                    initMetricEntity.setApp(app);
                    initMetricEntity.setTimestamp(new Date(timestamp));
                    initMetricEntity.setPassQps(0L);
                    initMetricEntity.setBlockQps(0L);
                    initMetricEntity.setRtAndSuccessQps(0, 0L);
                    initMetricEntity.setExceptionQps(0L);
                    initMetricEntity.setCount(0);
                    initMetricEntity.setResource(resource);
                    return initMetricEntity;
                });
                metricEntity.addPassQps(node.getPassQps());
                metricEntity.addBlockQps(node.getBlockQps());
                metricEntity.addRtAndSuccessQps(node.getRt(), node.getSuccessQps());
                metricEntity.addExceptionQps(node.getExceptionQps());
                metricEntity.addCount(1);
            } catch (Exception e) {
                logger.warn("handleBody line exception, machine: {}, line: {}", machine.toLogString(), line, e);
            }
        }
    }

    /** 生成 {@code app__resource__秒级时间戳} 形式的聚合 key */
    private String buildMetricKey(String app, String resource, long timestamp) {
        return app + "__" + resource + "__" + (timestamp / 1000);
    }

    /** 判断资源是否在排除集合中，命中则跳过该指标 */
    private boolean shouldFilterOut(String resource) {
        return RES_EXCLUSION_SET.contains(resource);
    }

    /** 不参与聚合的排除资源：总入口请求数、系统负载与 CPU 使用率 */
    private static final Set<String> RES_EXCLUSION_SET = new HashSet<String>() {{
       super.add(Constants.TOTAL_IN_RESOURCE_NAME);
       super.add(Constants.SYSTEM_LOAD_RESOURCE_NAME);
       super.add(Constants.CPU_USAGE_RESOURCE_NAME);
    }};

}



