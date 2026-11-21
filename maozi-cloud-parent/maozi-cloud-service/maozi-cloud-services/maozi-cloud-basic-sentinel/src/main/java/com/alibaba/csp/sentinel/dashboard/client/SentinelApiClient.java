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
package com.alibaba.csp.sentinel.dashboard.client;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.command.CommandConstants;
import com.alibaba.csp.sentinel.command.vo.NodeVo;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.SentinelVersion;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterClientInfoVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerFlowConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterServerStateVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterStateSimpleEntity;
import com.alibaba.csp.sentinel.dashboard.util.AsyncUtils;
import com.alibaba.csp.sentinel.dashboard.util.VersionUtils;
import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 与 Sentinel 客户端通信的 API 客户端。
 *
 * @author leyou
 * @date 2026/11/22
 */
@Component
public class SentinelApiClient {
    /** 日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(SentinelApiClient.class);

    /** 编码请求参数与解码响应体的默认字符集。 */
    private static final Charset DEFAULT_CHARSET = Charset.forName(SentinelConfig.charset());
    /** HTTP Content-Type 请求头名称。 */
    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    /** 普通 urlencoded 内容类型，目标客户端不支持增强形式时使用。 */
    private static final String HTTP_HEADER_CONTENT_TYPE_URLENCODED =
        ContentType.create(URLEncodedUtils.CONTENT_TYPE).toString();

    /** 获取资源树（JSON 格式）的命令路径。 */
    private static final String RESOURCE_URL_PATH = "jsonTree";
    /** 获取集群节点列表的命令路径。 */
    private static final String CLUSTER_NODE_PATH = "clusterNode";
    /** 获取通用规则（流控/降级/系统/授权）的命令路径。 */
    private static final String GET_RULES_PATH = "getRules";
    /** 修改通用规则（流控/降级/系统/授权）的命令路径。 */
    private static final String SET_RULES_PATH = "setRules";
    /** 获取热点参数规则的命令路径。 */
    private static final String GET_PARAM_RULE_PATH = "getParamFlowRules";
    /** 修改热点参数规则的命令路径。 */
    private static final String SET_PARAM_RULE_PATH = "setParamFlowRules";

    /** 获取集群模式的命令路径。 */
    private static final String FETCH_CLUSTER_MODE_PATH = "getClusterMode";
    /** 修改集群模式的命令路径。 */
    private static final String MODIFY_CLUSTER_MODE_PATH = "setClusterMode";
    /** 获取集群客户端配置的命令路径。 */
    private static final String FETCH_CLUSTER_CLIENT_CONFIG_PATH = "cluster/client/fetchConfig";
    /** 修改集群客户端配置的命令路径。 */
    private static final String MODIFY_CLUSTER_CLIENT_CONFIG_PATH = "cluster/client/modifyConfig";

    /** 获取集群服务端基本信息的命令路径。 */
    private static final String FETCH_CLUSTER_SERVER_BASIC_INFO_PATH = "cluster/server/info";

    /** 修改集群服务端通信配置的命令路径。 */
    private static final String MODIFY_CLUSTER_SERVER_TRANSPORT_CONFIG_PATH = "cluster/server/modifyTransportConfig";
    /** 修改集群服务端流控配置的命令路径。 */
    private static final String MODIFY_CLUSTER_SERVER_FLOW_CONFIG_PATH = "cluster/server/modifyFlowConfig";
    /** 修改集群服务端命名空间集合的命令路径。 */
    private static final String MODIFY_CLUSTER_SERVER_NAMESPACE_SET_PATH = "cluster/server/modifyNamespaceSet";

    /** 获取网关 API 分组的命令路径。 */
    private static final String FETCH_GATEWAY_API_PATH = "gateway/getApiDefinitions";
    /** 修改网关 API 分组的命令路径。 */
    private static final String MODIFY_GATEWAY_API_PATH = "gateway/updateApiDefinitions";

    /** 获取网关流控规则的命令路径。 */
    private static final String FETCH_GATEWAY_FLOW_RULE_PATH = "gateway/getRules";
    /** 修改网关流控规则的命令路径。 */
    private static final String MODIFY_GATEWAY_FLOW_RULE_PATH = "gateway/updateRules";

    /** 流控规则类型名。 */
    private static final String FLOW_RULE_TYPE = "flow";
    /** 降级规则类型名。 */
    private static final String DEGRADE_RULE_TYPE = "degrade";
    /** 系统规则类型名。 */
    private static final String SYSTEM_RULE_TYPE = "system";
    /** 授权规则类型名。 */
    private static final String AUTHORITY_TYPE = "authority";

    /** 表示命令成功的 HTTP 2xx 状态码下界（含）。 */
    private static final int HTTP_STATUS_OK_MIN = 200;
    /** 表示命令成功的 HTTP 2xx 状态码上界（不含）。 */
    private static final int HTTP_STATUS_OK_MAX = 300;
    /** HTTP 400 状态码，客户端不识别命令时返回。 */
    private static final int HTTP_STATUS_BAD_REQUEST = 400;
    /** Sentinel 客户端响应规则修改命令成功时回显的关键字。 */
    private static final String CLIENT_RESPONSE_SUCCESS = "success";

    /** 集群节点类型参数值：仅返回数值非零的节点。 */
    private static final String CLUSTER_NODE_TYPE_NOT_ZERO = "notZero";
    /** 集群节点类型参数值：包含数值为零的节点。 */
    private static final String CLUSTER_NODE_TYPE_ZERO = "zero";

    /** 与 Sentinel 客户端通信的 HTTP 连接超时时间（毫秒）。 */
    private static final int CONNECT_TIMEOUT_MS = 3000;
    /** 与 Sentinel 客户端通信的 HTTP 读超时时间（毫秒）。 */
    private static final int SO_TIMEOUT_MS = 10000;
    /** 底层异步 HTTP 客户端的最大总连接数。 */
    private static final int MAX_CONN_TOTAL = 4000;
    /** 底层异步 HTTP 客户端的单路由最大连接数。 */
    private static final int MAX_CONN_PER_ROUTE = 1000;

    /** 与 Sentinel 客户端通信的底层异步 HTTP 客户端。 */
    private CloseableHttpAsyncClient httpClient;

    /** Sentinel 1.6.0 版本，从此版本起支持以 POST 方式下发命令。 */
    private static final SentinelVersion VERSION_1_6_0 = new SentinelVersion(1, 6, 0);
    /** Sentinel 1.7.1 版本，从此版本起支持增强的 Content-Type 形式。 */
    private static final SentinelVersion VERSION_1_7_1 = new SentinelVersion(1, 7, 1);
    
    /** 应用与机器注册表，用于机器有效性及版本校验。 */
    private final AppManagement appManagement;

    /** 创建并启动底层异步 HTTP 客户端，禁用重定向。 */
    public SentinelApiClient(AppManagement appManagement) {
        this.appManagement = appManagement;
        IOReactorConfig ioConfig = IOReactorConfig.custom().setConnectTimeout(CONNECT_TIMEOUT_MS)
            .setSoTimeout(SO_TIMEOUT_MS)
            .setIoThreadCount(Runtime.getRuntime().availableProcessors() * 2).build();
        httpClient = HttpAsyncClients.custom().setRedirectStrategy(new DefaultRedirectStrategy() {
            /** 始终返回 false，禁用 Sentinel 客户端命令请求的重定向。 */
            @Override
            protected boolean isRedirectable(final String method) {
                return false;
            }
        }).setMaxConnTotal(MAX_CONN_TOTAL).setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
            .setDefaultIOReactorConfig(ioConfig).build();
        httpClient.start();
    }

    /** 判断指定 HTTP 状态码是否表示成功（2xx）。 */
    private boolean isSuccess(int statusCode) {
        return statusCode >= HTTP_STATUS_OK_MIN && statusCode < HTTP_STATUS_OK_MAX;
    }

    /** 判断失败响应是否表示客户端不识别该命令（状态码 400 且含未知命令消息）。 */
    private boolean isCommandNotFound(int statusCode, String body) {
        return statusCode == HTTP_STATUS_BAD_REQUEST && StringUtil.isNotEmpty(body) &&
            body.contains(CommandConstants.MSG_UNKNOWN_COMMAND_PREFIX);
    }
    
    /** 判断注册机器的客户端版本是否支持以 POST 方式接收命令（>= 1.6.0）。 */
    protected boolean isSupportPost(String app, String ip, int port) {
        return StringUtil.isNotEmpty(app) && Optional.ofNullable(appManagement.getDetailApp(app))
                .flatMap(e -> e.getMachine(ip, port))
                .flatMap(m -> VersionUtils.parseVersion(m.getVersion())
                    .map(v -> v.greaterOrEqual(VERSION_1_6_0)))
                .orElse(false);
    }
    
    /**
     * 判断目标实例（以 app-ip:port 标识）是否支持 Content-Type 请求头中的
     * "xxxxx; xx=xx" 增强形式。
     *
     * @param app  目标应用名
     * @param ip   目标节点地址
     * @param port 目标节点端口
     * @return 支持返回 true，否则返回 false
     */
    protected boolean isSupportEnhancedContentType(String app, String ip, int port) {
        return StringUtil.isNotEmpty(app) && Optional.ofNullable(appManagement.getDetailApp(app))
                .flatMap(e -> e.getMachine(ip, port))
                .flatMap(m -> VersionUtils.parseVersion(m.getVersion())
                    .map(v -> v.greaterOrEqual(VERSION_1_7_1)))
                .orElse(false);
    }
    
    /**
     * 构造 url 编码的查询串（不含前导 '?' 或 '&'），跳过键值为空或编码失败的条目。
     *
     * @param params 请求参数
     * @return 查询串构建器
     */
    private StringBuilder queryString(Map<String, String> params) {
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Entry<String, String> entry : params.entrySet()) {
            // 缓存 entry 的值，避免同一轮循环内重复调用
            String entryValue = entry.getValue();
            if (StringUtil.isEmpty(entryValue)) {
                continue;
            }
            String name = urlEncode(entry.getKey());
            String value = urlEncode(entryValue);
            if (name != null && value != null) {
                if (queryStringBuilder.length() > 0) {
                    queryStringBuilder.append('&');
                }
                queryStringBuilder.append(name).append('=').append(value);
            }
        }
        return queryStringBuilder;
    }
    
    /**
     * 构建以 url 编码表单实体携带参数的 POST 请求。
     *
     * @param url                        请求地址
     * @param params                     待发送的表单参数
     * @param supportEnhancedContentType 是否支持增强 Content-Type，见 {@link #isSupportEnhancedContentType(String, String, int)}
     * @return 构建好的 POST 请求
     */
    protected static HttpUriRequest postRequest(String url, Map<String, String> params, 
        boolean supportEnhancedContentType) {
        HttpPost httpPost = new HttpPost(url);
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> list = new ArrayList<>(params.size());
            for (Entry<String, String> entry : params.entrySet()) {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(list, Consts.UTF_8));
            if (!supportEnhancedContentType) {
                httpPost.setHeader(HTTP_HEADER_CONTENT_TYPE, HTTP_HEADER_CONTENT_TYPE_URLENCODED);
            }
        }
        return httpPost;
    }
    
    /**
     * 以默认字符集对字符串做 url 编码，编码失败返回 null。
     *
     * @param str 待编码字符串
     * @return 编码结果，失败时返回 null
     */
    private String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, DEFAULT_CHARSET.name());
        } catch (UnsupportedEncodingException e) {
            logger.info("encode string error: {}", str, e);
            return null;
        }
    }
    
    /**
     * 使用从 Content-Type 请求头解析出的字符集读取响应体，解析失败时回退为默认字符集。
     *
     * @param response HTTP 响应
     * @return 响应体字符串
     * @throws Exception 读取响应体失败时抛出
     */
    private String getBody(HttpResponse response) throws Exception {
        Charset charset = null;
        try {
            String contentTypeStr = response.getFirstHeader(HTTP_HEADER_CONTENT_TYPE).getValue();
            if (StringUtil.isNotEmpty(contentTypeStr)) {
                ContentType contentType = ContentType.parse(contentTypeStr);
                charset = contentType.getCharset();
            }
        } catch (Exception ignore) {
            // Content-Type 解析失败时回退默认字符集，debug 日志仅用于排查
            logger.debug("Parse Content-Type charset failed, fallback to {}", DEFAULT_CHARSET, ignore);
        }
        return EntityUtils.toString(response.getEntity(), charset != null ? charset : DEFAULT_CHARSET);
    }
    
    /**
     * 不带请求参数执行命令。
     *
     * @param ip         机器 IP
     * @param port       机器命令端口
     * @param api        命令名称
     * @param useHttpPost 是否优先使用 POST
     * @return 以响应体完成的 future
     */
    private CompletableFuture<String> executeCommand(String ip, int port, String api, boolean useHttpPost) {
        return executeCommand(ip, port, api, null, useHttpPost);
    }
    
    /**
     * 不带应用名执行命令；缺少应用名时机器版本未知，请求始终回退为 GET 形式。
     *
     * @param ip         机器 IP
     * @param port       机器命令端口
     * @param api        命令名称
     * @param params     查询参数
     * @param useHttpPost 是否优先使用 POST
     * @return 以响应体完成的 future
     */
    private CompletableFuture<String> executeCommand(String ip, int port, String api, Map<String, String> params, 
        boolean useHttpPost) {
        return executeCommand(null, ip, port, api, params, useHttpPost);
    }

    /**
     * 向目标机器执行命令：机器客户端版本支持 POST 时优先使用 POST，否则回退为 GET。
     *
     * @param app        机器所属应用名
     * @param ip         机器 IP
     * @param port       机器命令端口
     * @param api        命令名称
     * @param params     请求参数
     * @param useHttpPost 是否优先使用 POST
     * @return 以响应体完成的 future
     */
    private CompletableFuture<String> executeCommand(String app, String ip, int port, String api, 
        Map<String, String> params, boolean useHttpPost) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (StringUtil.isBlank(ip) || StringUtil.isBlank(api)) {
            future.completeExceptionally(new IllegalArgumentException("Bad URL or command name"));
            return future;
        }
        if (!InetAddressUtils.isIPv4Address(ip) && !InetAddressUtils.isIPv6Address(ip)) {
            future.completeExceptionally(new IllegalArgumentException("Bad IP"));
            return future;
        }
        if (!StringUtil.isEmpty(app) && !appManagement.isValidMachineOfApp(app, ip)) {
            future.completeExceptionally(new IllegalArgumentException("Given ip does not belong to given app"));
            return future;
        }
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://");
        urlBuilder.append(ip).append(':').append(port).append('/').append(api);
        if (params == null) {
            params = Collections.emptyMap();
        }
        if (!useHttpPost || !isSupportPost(app, ip, port)) {
            // 旧版本客户端使用 GET，参数拼接在 url 之后
            if (!params.isEmpty()) {
                if (urlBuilder.indexOf("?") == -1) {
                    urlBuilder.append('?');
                } else {
                    urlBuilder.append('&');
                }
                urlBuilder.append(queryString(params));
            }
            return executeCommand(new HttpGet(urlBuilder.toString()));
        } else {
            // 使用 POST 方式
            return executeCommand(
                    postRequest(urlBuilder.toString(), params, isSupportEnhancedContentType(app, ip, port)));
        }
    }
    
    /**
     * 异步执行请求：2xx 响应以响应体完成 future，其他状态码以 {@link CommandNotFoundException}
     * （命令未识别）或 {@link CommandFailedException} 使其失败，请求被取消时以 null 完成。
     *
     * @param request HTTP 请求
     * @return 以响应体完成的 future
     */
    private CompletableFuture<String> executeCommand(HttpUriRequest request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        httpClient.execute(request, new FutureCallback<HttpResponse>() {
            /** 2xx 响应以响应体完成 future，否则以对应异常使其失败。 */
            @Override
            public void completed(final HttpResponse response) {
                int statusCode = response.getStatusLine().getStatusCode();
                try {
                    String value = getBody(response);
                    if (isSuccess(statusCode)) {
                        future.complete(value);
                    } else {
                        if (isCommandNotFound(statusCode, value)) {
                            future.completeExceptionally(new CommandNotFoundException(request.getURI().getPath()));
                        } else {
                            future.completeExceptionally(new CommandFailedException(value));
                        }
                    }

                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                    logger.error("HTTP request failed: {}", request.getURI().toString(), ex);
                }
            }

            /** 请求异常时以该异常使 future 失败并记录日志。 */
            @Override
            public void failed(final Exception ex) {
                future.completeExceptionally(ex);
                logger.error("HTTP request failed: {}", request.getURI().toString(), ex);
            }

            /** 请求被取消时以 null 完成 future。 */
            @Override
            public void cancelled() {
                future.complete(null);
            }
        });
        return future;
    }
    
    /**
     * 关闭底层异步 HTTP 客户端。
     *
     * @throws Exception 关闭失败时抛出
     */
    public void close() throws Exception {
        httpClient.close();
    }
    
    /**
     * 通过指定命令（GET 方式）获取 JSON 数组，并将每个元素解析为指定类型。
     *
     * @param ip       机器 IP
     * @param port     机器命令端口
     * @param api      命令名称
     * @param type     规则类型参数，可为 null
     * @param ruleType 目标元素类型
     * @param <T>      目标元素类型
     * @return 以解析结果完成的 future
     */
    @Nullable
    private <T> CompletableFuture<List<T>> fetchItemsAsync(String ip, int port, String api, String type, 
        Class<T> ruleType) {
        AssertUtil.notEmpty(ip, "Bad machine IP");
        AssertUtil.isTrue(port > 0, "Bad machine port");
        Map<String, String> params = null;
        if (StringUtil.isNotEmpty(type)) {
            params = new HashMap<>(1);
            params.put("type", type);
        }
        return executeCommand(ip, port, api, params, false)
                .thenApply(json -> JSON.parseArray(json, ruleType));
    }
    
    /**
     * {@link #fetchItemsAsync} 的同步版本；出错时不抛异常，返回空列表。
     *
     * @param ip       机器 IP
     * @param port     机器命令端口
     * @param api      命令名称
     * @param type     规则类型参数，可为 null
     * @param ruleType 目标元素类型
     * @param <T>      目标元素类型
     * @return 解析结果列表，失败时返回空列表
     */
    @Nullable
    private <T> List<T> fetchItems(String ip, int port, String api, String type, Class<T> ruleType) {
        try {
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            Map<String, String> params;
            if (StringUtil.isNotEmpty(type)) {
                params = new HashMap<>(1);
                params.put("type", type);
            }
            return fetchItemsAsync(ip, port, api, type, ruleType).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error when fetching items from api: {} -> {}", api, type, e);
            return Collections.emptyList();
        } catch (ExecutionException e) {
            logger.error("Error when fetching items from api: {} -> {}", api, type, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error when fetching items: {} -> {}", api, type, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 通过 getRules 命令获取指定类型的规则列表。
     *
     * @param ip       机器 IP
     * @param port     机器命令端口
     * @param type     规则类型
     * @param ruleType 规则目标类型
     * @param <T>      规则类型
     * @return 规则列表
     */
    private <T extends Rule> List<T> fetchRules(String ip, int port, String type, Class<T> ruleType) {
        return fetchItems(ip, port, GET_RULES_PATH, type, ruleType);
    }
    
    /**
     * 通过 setRules 命令推送指定类型的规则；规则列表为 null 时直接跳过并返回 true。
     *
     * @param app      应用名
     * @param ip       机器 IP
     * @param port     机器命令端口
     * @param type     规则类型
     * @param entities 规则实体列表
     * @return 推送成功返回 true，失败返回 false
     */
    private boolean setRules(String app, String ip, int port, String type, List<? extends RuleEntity> entities) {
        if (entities == null) {
            return true;
        }
        try {
            AssertUtil.notEmpty(app, "Bad app name");
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            String data = JSON.toJSONString(
                    entities.stream().map(r -> r.toRule()).collect(Collectors.toList()));
            Map<String, String> params = new HashMap<>(2);
            params.put("type", type);
            params.put("data", data);
            String result = executeCommand(app, ip, port, SET_RULES_PATH, params, true).get();
            logger.info("setRules result: {}, type={}", result, type);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("setRules API failed: {}", type, e);
            return false;
        } catch (ExecutionException e) {
            logger.warn("setRules API failed: {}", type, e.getCause());
            return false;
        } catch (Exception e) {
            logger.error("setRules API failed, type={}", type, e);
            return false;
        }
    }

    /**
     * {@link #setRules} 的异步版本；要求客户端回复 success，规则列表为 null 时使 future 失败。
     *
     * @param app      应用名
     * @param ip       机器 IP
     * @param port     机器命令端口
     * @param type     规则类型
     * @param entities 规则实体列表
     * @return 推送结果的 future
     */
    private CompletableFuture<Void> setRulesAsync(String app, String ip, int port, String type, 
        List<? extends RuleEntity> entities) {
        try {
            AssertUtil.notNull(entities, "rules cannot be null");
            AssertUtil.notEmpty(app, "Bad app name");
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            String data = JSON.toJSONString(
                entities.stream().map(r -> r.toRule()).collect(Collectors.toList()));
            Map<String, String> params = new HashMap<>(2);
            params.put("type", type);
            params.put("data", data);
            return executeCommand(app, ip, port, SET_RULES_PATH, params, true)
                .thenCompose(r -> {
                    if (CLIENT_RESPONSE_SUCCESS.equalsIgnoreCase(r.trim())) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return AsyncUtils.newFailedFuture(new CommandFailedException(r));
                });
        } catch (Exception e) {
            logger.error("setRulesAsync API failed, type={}", type, e);
            return AsyncUtils.newFailedFuture(e);
        }
    }

    /**
     * 获取机器的资源树（jsonTree），可按指定资源类型过滤。
     *
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @param type 资源类型过滤条件，可为 null
     * @return 资源树节点列表
     */
    public List<NodeVo> fetchResourceOfMachine(String ip, int port, String type) {
        return fetchItems(ip, port, RESOURCE_URL_PATH, type, NodeVo.class);
    }

    /**
     * 获取机器的集群节点列表，可选择包含数值为零的节点。
     *
     * @param ip          机器 IP
     * @param port        机器命令端口
     * @param includeZero 是否包含数值为零的节点
     * @return 集群节点列表，获取失败时返回空列表
     */
    public List<NodeVo> fetchClusterNodeOfMachine(String ip, int port, boolean includeZero) {
        String type = includeZero ? CLUSTER_NODE_TYPE_ZERO : CLUSTER_NODE_TYPE_NOT_ZERO;
        return fetchItems(ip, port, CLUSTER_NODE_PATH, type, NodeVo.class);
    }

    /**
     * 获取机器的全部流控规则并转换为实体，获取失败时返回空列表。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @return 流控规则实体列表
     */
    public List<FlowRuleEntity> fetchFlowRuleOfMachine(String app, String ip, int port) {
        List<FlowRule> rules = fetchRules(ip, port, FLOW_RULE_TYPE, FlowRule.class);
        if (rules != null) {
            return rules.stream().map(rule -> FlowRuleEntity.fromFlowRule(app, ip, port, rule))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 获取机器的全部降级规则并转换为实体，获取失败时返回空列表。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @return 降级规则实体列表
     */
    public List<DegradeRuleEntity> fetchDegradeRuleOfMachine(String app, String ip, int port) {
        List<DegradeRule> rules = fetchRules(ip, port, DEGRADE_RULE_TYPE, DegradeRule.class);
        if (rules != null) {
            return rules.stream().map(rule -> DegradeRuleEntity.fromDegradeRule(app, ip, port, rule))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 获取机器的全部系统规则并转换为实体，获取失败时返回空列表。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @return 系统规则实体列表
     */
    public List<SystemRuleEntity> fetchSystemRuleOfMachine(String app, String ip, int port) {
        List<SystemRule> rules = fetchRules(ip, port, SYSTEM_RULE_TYPE, SystemRule.class);
        if (rules != null) {
            return rules.stream().map(rule -> SystemRuleEntity.fromSystemRule(app, ip, port, rule))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定机器的全部热点参数规则。
     *
     * @param app  应用名
     * @param ip   机器客户端 IP
     * @param port 机器客户端端口
     * @return 以全部热点参数规则完成的 future
     * @since 0.2.1
     */
    public CompletableFuture<List<ParamFlowRuleEntity>> fetchParamFlowRulesOfMachine(String app, String ip, int port) {
        try {
            AssertUtil.notEmpty(app, "Bad app name");
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            return fetchItemsAsync(ip, port, GET_PARAM_RULE_PATH, null, ParamFlowRule.class)
                .thenApply(rules -> rules.stream()
                    .map(e -> ParamFlowRuleEntity.fromParamFlowRule(app, ip, port, e))
                    .collect(Collectors.toList())
                );
        } catch (Exception e) {
            logger.error("Error when fetching parameter flow rules", e);
            return AsyncUtils.newFailedFuture(e);
        }
    }

    /**
     * 获取指定机器的全部授权规则。
     *
     * @param app  应用名
     * @param ip   机器客户端 IP
     * @param port 机器客户端端口
     * @return 授权规则实体列表
     * @since 0.2.1
     */
    public List<AuthorityRuleEntity> fetchAuthorityRulesOfMachine(String app, String ip, int port) {
        AssertUtil.notEmpty(app, "Bad app name");
        AssertUtil.notEmpty(ip, "Bad machine IP");
        AssertUtil.isTrue(port > 0, "Bad machine port");
        List<AuthorityRule> rules = fetchRules(ip, port, AUTHORITY_TYPE, AuthorityRule.class);
        return Optional.ofNullable(rules).map(r -> r.stream()
                    .map(e -> AuthorityRuleEntity.fromAuthorityRule(app, ip, port, e))
                    .collect(Collectors.toList())
                ).orElse(Collections.emptyList());
    }

    /**
     * 设置机器的流控规则；规则列表为 null 时直接返回，空列表表示清空规则。
     *
     * @param app   应用名
     * @param ip    机器 IP
     * @param port  机器命令端口
     * @param rules 待设置的规则列表
     * @return 设置成功返回 true，失败返回 false
     */
    public boolean setFlowRuleOfMachine(String app, String ip, int port, List<FlowRuleEntity> rules) {
        return setRules(app, ip, port, FLOW_RULE_TYPE, rules);
    }

    /**
     * 通过 {@link #setRulesAsync} 异步推送流控规则；规则列表为 null 时使 future 失败。
     *
     * @param app   应用名
     * @param ip    机器 IP
     * @param port  机器命令端口
     * @param rules 待推送的规则列表
     * @return 推送结果的 future
     */
    public CompletableFuture<Void> setFlowRuleOfMachineAsync(String app, String ip, int port, 
        List<FlowRuleEntity> rules) {
        return setRulesAsync(app, ip, port, FLOW_RULE_TYPE, rules);
    }

    /**
     * 设置机器的降级规则；规则列表为 null 时直接返回，空列表表示清空规则。
     *
     * @param app   应用名
     * @param ip    机器 IP
     * @param port  机器命令端口
     * @param rules 待设置的规则列表
     * @return 设置成功返回 true，失败返回 false
     */
    public boolean setDegradeRuleOfMachine(String app, String ip, int port, List<DegradeRuleEntity> rules) {
        return setRules(app, ip, port, DEGRADE_RULE_TYPE, rules);
    }

    /**
     * 设置机器的系统规则；规则列表为 null 时直接返回，空列表表示清空规则。
     *
     * @param app   应用名
     * @param ip    机器 IP
     * @param port  机器命令端口
     * @param rules 待设置的规则列表
     * @return 设置成功返回 true，失败返回 false
     */
    public boolean setSystemRuleOfMachine(String app, String ip, int port, List<SystemRuleEntity> rules) {
        return setRules(app, ip, port, SYSTEM_RULE_TYPE, rules);
    }

    /**
     * 设置机器的授权规则；规则列表为 null 时直接返回，空列表表示清空规则。
     *
     * @param app   应用名
     * @param ip    机器 IP
     * @param port  机器命令端口
     * @param rules 待设置的规则列表
     * @return 设置成功返回 true，失败返回 false
     */
    public boolean setAuthorityRuleOfMachine(String app, String ip, int port, List<AuthorityRuleEntity> rules) {
        return setRules(app, ip, port, AUTHORITY_TYPE, rules);
    }

    /**
     * 异步推送热点参数规则；规则列表为 null 时直接完成 future 而不推送。
     *
     * @param app   应用名
     * @param ip    机器 IP
     * @param port  机器命令端口
     * @param rules 待推送的规则列表
     * @return 推送结果的 future
     */
    public CompletableFuture<Void> setParamFlowRuleOfMachine(String app, String ip, int port, 
        List<ParamFlowRuleEntity> rules) {
        if (rules == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            String data = JSON.toJSONString(
                rules.stream().map(ParamFlowRuleEntity::getRule).collect(Collectors.toList())
            );
            Map<String, String> params = new HashMap<>(1);
            params.put("data", data);
            return executeCommand(app, ip, port, SET_PARAM_RULE_PATH, params, true)
                .thenCompose(e -> {
                    if (CommandConstants.MSG_SUCCESS.equals(e)) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        logger.warn("Push parameter flow rules to client failed: {}", e);
                        return AsyncUtils.newFailedFuture(new RuntimeException(e));
                    }
                });
        } catch (Exception ex) {
            logger.warn("Error when setting parameter flow rule", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    // 集群相关

    /**
     * 获取机器当前的集群模式状态。
     *
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @return 以集群模式状态完成的 future
     */
    public CompletableFuture<ClusterStateSimpleEntity> fetchClusterMode(String ip, int port) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            return executeCommand(ip, port, FETCH_CLUSTER_MODE_PATH, false)
                .thenApply(r -> JSON.parseObject(r, ClusterStateSimpleEntity.class));
        } catch (Exception ex) {
            logger.warn("Error when fetching cluster mode", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 修改机器的集群模式（客户端或服务端）。
     *
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @param mode 目标集群模式
     * @return 修改结果的 future
     */
    public CompletableFuture<Void> modifyClusterMode(String ip, int port, int mode) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            Map<String, String> params = new HashMap<>(1);
            params.put("mode", String.valueOf(mode));
            return executeCommand(ip, port, MODIFY_CLUSTER_MODE_PATH, params, false)
                .thenCompose(e -> {
                    if (CommandConstants.MSG_SUCCESS.equals(e)) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        logger.warn("Error when modifying cluster mode: {}", e);
                        return AsyncUtils.newFailedFuture(new RuntimeException(e));
                    }
                });
        } catch (Exception ex) {
            logger.warn("Error when modifying cluster mode", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 获取机器的集群客户端信息与配置。
     *
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @return 以集群客户端信息完成的 future
     */
    public CompletableFuture<ClusterClientInfoVO> fetchClusterClientInfoAndConfig(String ip, int port) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            return executeCommand(ip, port, FETCH_CLUSTER_CLIENT_CONFIG_PATH, false)
                .thenApply(r -> JSON.parseObject(r, ClusterClientInfoVO.class));
        } catch (Exception ex) {
            logger.warn("Error when fetching cluster client config", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 修改机器的集群客户端配置。
     *
     * @param app    应用名
     * @param ip     机器 IP
     * @param port   机器命令端口
     * @param config 集群客户端配置
     * @return 修改结果的 future
     */
    public CompletableFuture<Void> modifyClusterClientConfig(String app, String ip, int port, 
        ClusterClientConfig config) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            Map<String, String> params = new HashMap<>(1);
            params.put("data", JSON.toJSONString(config));
            return executeCommand(app, ip, port, MODIFY_CLUSTER_CLIENT_CONFIG_PATH, params, true)
                .thenCompose(e -> {
                    if (CommandConstants.MSG_SUCCESS.equals(e)) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        logger.warn("Error when modifying cluster client config: {}", e);
                        return AsyncUtils.newFailedFuture(new RuntimeException(e));
                    }
                });
        } catch (Exception ex) {
            logger.warn("Error when modifying cluster client config", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 修改机器的集群服务端流控配置。
     *
     * @param app    应用名
     * @param ip     机器 IP
     * @param port   机器命令端口
     * @param config 集群服务端流控配置
     * @return 修改结果的 future
     */
    public CompletableFuture<Void> modifyClusterServerFlowConfig(String app, String ip, int port, 
        ServerFlowConfig config) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            Map<String, String> params = new HashMap<>(1);
            params.put("data", JSON.toJSONString(config));
            return executeCommand(app, ip, port, MODIFY_CLUSTER_SERVER_FLOW_CONFIG_PATH, params, true)
                .thenCompose(e -> {
                    if (CommandConstants.MSG_SUCCESS.equals(e)) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        logger.warn("Error when modifying cluster server flow config: {}", e);
                        return AsyncUtils.newFailedFuture(new RuntimeException(e));
                    }
                });
        } catch (Exception ex) {
            logger.warn("Error when modifying cluster server flow config", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 修改机器的集群服务端通信配置（端口与空闲时间）。
     *
     * @param app    应用名
     * @param ip     机器 IP
     * @param port   机器命令端口
     * @param config 集群服务端通信配置
     * @return 修改结果的 future
     */
    public CompletableFuture<Void> modifyClusterServerTransportConfig(String app, String ip, int port, 
        ServerTransportConfig config) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            Map<String, String> params = new HashMap<>(2);
            params.put("port", config.getPort().toString());
            params.put("idleSeconds", config.getIdleSeconds().toString());
            return executeCommand(app, ip, port, MODIFY_CLUSTER_SERVER_TRANSPORT_CONFIG_PATH, params, false)
                .thenCompose(e -> {
                    if (CommandConstants.MSG_SUCCESS.equals(e)) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        logger.warn("Error when modifying cluster server transport config: {}", e);
                        return AsyncUtils.newFailedFuture(new RuntimeException(e));
                    }
                });
        } catch (Exception ex) {
            logger.warn("Error when modifying cluster server transport config", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 修改机器的集群服务端命名空间集合。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @param set  命名空间集合
     * @return 修改结果的 future
     */
    public CompletableFuture<Void> modifyClusterServerNamespaceSet(String app, String ip, int port, Set<String> set) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            Map<String, String> params = new HashMap<>(1);
            params.put("data", JSON.toJSONString(set));
            return executeCommand(app, ip, port, MODIFY_CLUSTER_SERVER_NAMESPACE_SET_PATH, params, true)
                .thenCompose(e -> {
                    if (CommandConstants.MSG_SUCCESS.equals(e)) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        logger.warn("Error when modifying cluster server NamespaceSet: {}", e);
                        return AsyncUtils.newFailedFuture(new RuntimeException(e));
                    }
                });
        } catch (Exception ex) {
            logger.warn("Error when modifying cluster server NamespaceSet", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 获取机器的集群服务端基本信息（全部配置与状态）。
     *
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @return 以集群服务端状态完成的 future
     */
    public CompletableFuture<ClusterServerStateVO> fetchClusterServerBasicInfo(String ip, int port) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            return executeCommand(ip, port, FETCH_CLUSTER_SERVER_BASIC_INFO_PATH, false)
                .thenApply(r -> JSON.parseObject(r, ClusterServerStateVO.class));
        } catch (Exception ex) {
            logger.warn("Error when fetching cluster sever all config and basic info", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 获取机器的网关 API 分组，并为每个实体填充 app/ip/port 信息。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @return 以 API 分组列表完成的 future
     */
    public CompletableFuture<List<ApiDefinitionEntity>> fetchApis(String app, String ip, int port) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }

        try {
            return executeCommand(ip, port, FETCH_GATEWAY_API_PATH, false)
                    .thenApply(r -> {
                        List<ApiDefinitionEntity> entities = JSON.parseArray(r, ApiDefinitionEntity.class);
                        if (entities != null) {
                            for (ApiDefinitionEntity entity : entities) {
                                entity.setApp(app);
                                entity.setIp(ip);
                                entity.setPort(port);
                            }
                        }
                        return entities;
                    });
        } catch (Exception ex) {
            logger.warn("Error when fetching gateway apis", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 向机器推送网关 API 分组；列表为 null 时直接跳过并返回 true。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @param apis API 分组列表
     * @return 推送成功返回 true，失败返回 false
     */
    public boolean modifyApis(String app, String ip, int port, List<ApiDefinitionEntity> apis) {
        if (apis == null) {
            return true;
        }

        try {
            AssertUtil.notEmpty(app, "Bad app name");
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            String data = JSON.toJSONString(
                    apis.stream().map(r -> r.toApiDefinition()).collect(Collectors.toList()));
            Map<String, String> params = new HashMap<>(2);
            params.put("data", data);
            String result = executeCommand(app, ip, port, MODIFY_GATEWAY_API_PATH, params, true).get();
            logger.info("Modify gateway apis: {}", result);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Error when modifying gateway apis", e);
            return false;
        } catch (Exception e) {
            logger.warn("Error when modifying gateway apis", e);
            return false;
        }
    }

    /**
     * 获取机器的网关流控规则并转换为实体。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器命令端口
     * @return 以网关流控规则实体列表完成的 future
     */
    public CompletableFuture<List<GatewayFlowRuleEntity>> fetchGatewayFlowRules(String app, String ip, int port) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }

        try {
            return executeCommand(ip, port, FETCH_GATEWAY_FLOW_RULE_PATH, false)
                    .thenApply(r -> {
                        List<GatewayFlowRule> gatewayFlowRules = JSON.parseArray(r, GatewayFlowRule.class);
                        List<GatewayFlowRuleEntity> entities = gatewayFlowRules.stream()
                            .map(rule -> GatewayFlowRuleEntity.fromGatewayFlowRule(app, ip, port, rule))
                            .collect(Collectors.toList());
                        return entities;
                    });
        } catch (Exception ex) {
            logger.warn("Error when fetching gateway flow rules", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 向机器推送网关流控规则；列表为 null 时直接跳过并返回 true。
     *
     * @param app   应用名
     * @param ip    机器 IP
     * @param port  机器命令端口
     * @param rules 待推送的规则列表
     * @return 推送成功返回 true，失败返回 false
     */
    public boolean modifyGatewayFlowRules(String app, String ip, int port, List<GatewayFlowRuleEntity> rules) {
        if (rules == null) {
            return true;
        }

        try {
            AssertUtil.notEmpty(app, "Bad app name");
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            String data = JSON.toJSONString(
                    rules.stream().map(r -> r.toGatewayFlowRule()).collect(Collectors.toList()));
            Map<String, String> params = new HashMap<>(2);
            params.put("data", data);
            String result = executeCommand(app, ip, port, MODIFY_GATEWAY_FLOW_RULE_PATH, params, true).get();
            logger.info("Modify gateway flow rules: {}", result);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Error when modifying gateway flow rules", e);
            return false;
        } catch (Exception e) {
            logger.warn("Error when modifying gateway flow rules", e);
            return false;
        }
    }
}
