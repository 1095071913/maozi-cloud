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
 * Fetch metric of machines.
 *
 * @author leyou
 */
@Component
public class MetricFetcher {

    /** Body prefix returned by the client when no metric exists in the requested window. */
    public static final String NO_METRICS = "No metrics";
    /** HTTP status code that marks a successful fetch. */
    private static final int HTTP_OK = 200;
    /** Max look-back window used when no valid last fetch time is recorded for an app. */
    private static final long MAX_LAST_FETCH_INTERVAL_MS = 1000 * 15;
    /** Length in seconds of each fetch time window. */
    private static final long FETCH_INTERVAL_SECOND = 6;
    /** Connect timeout in milliseconds for the fetch HTTP requests. */
    private static final int HTTP_CONNECT_TIMEOUT_MS = 3000;
    /** Socket read timeout in milliseconds for the fetch HTTP requests. */
    private static final int HTTP_SO_TIMEOUT_MS = 3000;
    /** Max total connections of the async HTTP client. */
    private static final int MAX_CONN_TOTAL = 4000;
    /** Max connections per route of the async HTTP client. */
    private static final int MAX_CONN_PER_ROUTE = 1000;
    /** Initial delay in seconds of the scheduled fetch task. */
    private static final long FETCH_INITIAL_DELAY_SECONDS = 10;
    /** Initial capacity of the metric aggregation map. */
    private static final int METRIC_MAP_INITIAL_CAPACITY = 16;
    /** CPU core multiplier used to size the fetch and IO thread pools. */
    private static final int CPU_CORE_MULTIPLIER = 2;
    /** Milliseconds in one second. */
    private static final long ONE_SECOND_MS = 1000L;
    /** Skip the round when the fetch window ends within this many seconds of now. */
    private static final int NEAR_SKIP_WINDOW_SECONDS = 2;
    /** Max seconds a single fetch waits for the HTTP client. */
    private static final int MAX_WAIT_SECONDS = 5;
    /** Task queue capacity of the fetch dispatch thread pool. */
    private static final int FETCH_QUEUE_SIZE = 2048;
    /** Charset used to decode the response body when it declares none. */
    private static final Charset DEFAULT_CHARSET = Charset.forName(SentinelConfig.charset());
    /** Path of the metric command on the Sentinel client. */
    private static final String METRIC_URL_PATH = "metric";
    /** Logger of this class. */
    private static final Logger logger = LoggerFactory.getLogger(MetricFetcher.class);
    /** Period in seconds of the scheduled fetch-all task. */
    private final long intervalSecond = 1;

    /** End time (ms) of the latest fetch window per app name, used to compute the next window. */
    private Map<String, AtomicLong> appLastFetchTime = new ConcurrentHashMap<>();

    /** Metric repository used to persist the fetched metric entities. */
    private final MetricsRepository<MetricEntity> metricStore;
    /** Registry of applications and machines, used to enumerate the fetch targets. */
    private final AppManagement appManagement;

    /** Shared async HTTP client used to pull metrics from the Sentinel clients. */
    private CloseableHttpAsyncClient httpclient;

    /** Scheduler that periodically triggers metric fetching of all apps. */
    @SuppressWarnings("PMD.ThreadPoolCreationRule")
    private ScheduledExecutorService fetchScheduleService = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("sentinel-dashboard-metrics-fetch-task", true),
            new ThreadPoolExecutor.AbortPolicy());
    /** Executor that runs the per-app metric fetch dispatch tasks. */
    private ExecutorService fetchService;
    /** Executor that runs the actual per-app fetch tasks (HTTP round trips). */
    private ExecutorService fetchWorker;

    /**
     * Creates the fetch executors and the shared async HTTP client,
     * then starts the periodic fetch-all schedule.
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
            // reuse the cached core count to avoid a second runtime query
            .setIoThreadCount(cores)
            .build();

        httpclient = HttpAsyncClients.custom()
            .setRedirectStrategy(new DefaultRedirectStrategy() {
                /** Never follow redirects when fetching metrics. */
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

    /** Schedules {@link #fetchAllApp} every {@link #intervalSecond} seconds after a 10-second initial delay. */
    private void start() {
        fetchScheduleService.scheduleAtFixedRate(() -> {
            try {
                fetchAllApp();
            } catch (Exception e) {
                logger.info("fetchAllApp error:", e);
            }
        }, FETCH_INITIAL_DELAY_SECONDS, intervalSecond, TimeUnit.SECONDS);
    }

    /** Stamps gmtCreate/gmtModified with now and persists the metrics in batch; empty input is ignored. */
    private void writeMetric(Map<String, MetricEntity> map) {
        if (map.isEmpty()) {
            return;
        }
        Date date = new Date();
        // Cache the metric collection to avoid repeated calls within the same method.
        Collection<MetricEntity> metrics = map.values();
        for (MetricEntity entity : metrics) {
            entity.setGmtCreate(date);
            entity.setGmtModified(date);
        }
        metricStore.saveAll(metrics);
    }

    /**
     * Traverse each APP, and then pull the metric of all machines for that APP.
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
     * fetch metric between [startTime, endTime], both side inclusive
     */
    private void fetchOnce(String app, long startTime, long endTime, int maxWaitSeconds) {
        if (maxWaitSeconds <= 0) {
            throw new IllegalArgumentException("maxWaitSeconds must > 0, but " + maxWaitSeconds);
        }
        AppInfo appInfo = appManagement.getDetailApp(app);
        // auto remove for app
        if (appInfo.isDead()) {
            logger.info("Dead app removed: {}", app);
            appManagement.removeApp(app);
            return;
        }
        Set<MachineInfo> machines = appInfo.getMachines();
        // Cache the machine count to avoid repeated calls within the same method.
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

        // app_resource_timeSecond -> metric
        final Map<String, MetricEntity> metricMap = new ConcurrentHashMap<>(METRIC_MAP_INITIAL_CAPACITY);
        final CountDownLatch latch = new CountDownLatch(machineCount);
        for (final MachineInfo machine : machines) {
            // auto remove the machine whose health status is dead (lost)
            if (!machine.isHealthy()) {
                latch.countDown();
                unhealthy.incrementAndGet();
                // Cache the machine ip and port to avoid repeated calls within the same iteration.
                String machineIp = machine.getIp();
                int machinePort = machine.getPort();
                appManagement.getDetailApp(app).removeMachine(machineIp, machinePort);
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
     * Sends an async metric fetch request to the given machine; the callback parses the
     * response into the shared metric map and always counts down the latch.
     */
    private void asyncFetchMachineMetric(final MachineInfo machine, long startTime, long endTime,
            final Map<String, MetricEntity> metricMap, final CountDownLatch latch, final String msg,
            final AtomicLong success, final AtomicLong fail) {
        final String url = "http://" + machine.getIp() + ":" + machine.getPort() + "/" + METRIC_URL_PATH
            + "?startTime=" + startTime + "&endTime=" + endTime + "&refetch=" + false;
        final HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        httpclient.execute(httpGet, new FutureCallback<HttpResponse>() {
            /** Hands the successful response to handleResponse and counts down the latch. */
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

            /** Counts down the latch, aborts the request and logs the failure by exception type. */
            @Override
            public void failed(final Exception ex) {
                latch.countDown();
                fail.incrementAndGet();
                httpGet.abort();
                if (ex instanceof SocketTimeoutException) {
                    logger.error("Failed to fetch metric from <{}>: socket timeout", url);
                } else if (ex instanceof ConnectException) {
                    logger.error("Failed to fetch metric from <{}> (ConnectionException: {})",
                        url, ex.getMessage());
                } else {
                    logger.error("{} metric {} error", msg, url, ex);
                }
            }

            /** Counts down the latch, aborts the request and records it as a failure. */
            @Override
            public void cancelled() {
                latch.countDown();
                fail.incrementAndGet();
                httpGet.abort();
            }
        });
    }

    /**
     * Computes the next fetch window from the recorded last fetch time (bounded by
     * {@link #MAX_LAST_FETCH_INTERVAL_MS}) and submits the fetch to {@link #fetchWorker};
     * a window ending less than 2 seconds ago is skipped and left to the next round.
     */
    private void doFetchAppMetric(final String app) {
        long now = System.currentTimeMillis();
        long lastFetchMs = now - MAX_LAST_FETCH_INTERVAL_MS;
        if (appLastFetchTime.containsKey(app)) {
            lastFetchMs = Math.max(lastFetchMs, appLastFetchTime.get(app).get() + ONE_SECOND_MS);
        }
        // trim milliseconds
        lastFetchMs = lastFetchMs / ONE_SECOND_MS * ONE_SECOND_MS;
        long endTime = lastFetchMs + FETCH_INTERVAL_SECOND * ONE_SECOND_MS;
        if (endTime > now - ONE_SECOND_MS * NEAR_SKIP_WINDOW_SECONDS) {
            // too near
            return;
        }
        // update last_fetch in advance.
        appLastFetchTime.computeIfAbsent(app, a -> new AtomicLong()).set(endTime);
        final long finalLastFetchMs = lastFetchMs;
        final long finalEndTime = endTime;
        try {
            // do real fetch async
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
     * Handles one fetch response: ignores non-200, decodes with the Content-Type charset
     * (falling back to {@link #DEFAULT_CHARSET}), skips empty or {@link #NO_METRICS}
     * bodies, and parses the remaining lines via {@link #handleBody}.
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
            // Content-Type parsing failed: fall back to DEFAULT_CHARSET below; debug trace for troubleshooting
            logger.debug("Parse Content-Type charset failed, fallback to {}", DEFAULT_CHARSET, ignore);
        }
        String body = EntityUtils.toString(response.getEntity(), charset != null ? charset : DEFAULT_CHARSET);
        if (StringUtil.isEmpty(body) || body.startsWith(NO_METRICS)) {
            //logger.info(machine.getApp() + ":" + machine.getIp() + ":" + machine.getPort() + ", bodyStr is empty");
            return;
        }
        String[] lines = body.split("\n");
        //logger.info(machine.getApp() + ":" + machine.getIp() + ":" + machine.getPort() +
        //    ", bodyStr.length()=" + body.length() + ", lines=" + lines.length);
        handleBody(lines, machine, metricMap);
    }

    /**
     * Parses each metric line into a {@link MetricNode}, skips excluded resources and
     * aggregates the values into the map keyed by app__resource__timeSecond, merging
     * the metrics of all machines of the app (ignoring ip and port).
     */
    private void handleBody(String[] lines, MachineInfo machine, Map<String, MetricEntity> map) {
        //logger.info("handleBody() lines=" + lines.length + ", machine=" + machine);
        if (lines.length < 1) {
            return;
        }

        for (String line : lines) {
            try {
                MetricNode node = MetricNode.fromThinString(line);
                // Cache the resource name to avoid repeated calls within the same iteration.
                String resource = node.getResource();
                if (shouldFilterOut(resource)) {
                    continue;
                }
                /*
                 * aggregation metrics by app_resource_timeSecond, ignore ip and port.
                 */
                String key = buildMetricKey(machine.getApp(), resource, node.getTimestamp());

                MetricEntity metricEntity = map.computeIfAbsent(key, s -> {
                    MetricEntity initMetricEntity = new MetricEntity();
                    initMetricEntity.setApp(machine.getApp());
                    initMetricEntity.setTimestamp(new Date(node.getTimestamp()));
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
                logger.warn("handleBody line exception, machine: {}, line: {}", machine.toLogString(), line);
            }
        }
    }

    /** Returns the aggregation key in the form {@code app__resource__timestampInSecond}. */
    private String buildMetricKey(String app, String resource, long timestamp) {
        return app + "__" + resource + "__" + (timestamp / 1000);
    }

    /** Returns whether the resource is in the exclusion set and should be skipped. */
    private boolean shouldFilterOut(String resource) {
        return RES_EXCLUSION_SET.contains(resource);
    }

    /** Resources excluded from aggregation: total inbound requests, system load and CPU usage. */
    private static final Set<String> RES_EXCLUSION_SET = new HashSet<String>() {{
       super.add(Constants.TOTAL_IN_RESOURCE_NAME);
       super.add(Constants.SYSTEM_LOAD_RESOURCE_NAME);
       super.add(Constants.CPU_USAGE_RESOURCE_NAME);
    }};

}



