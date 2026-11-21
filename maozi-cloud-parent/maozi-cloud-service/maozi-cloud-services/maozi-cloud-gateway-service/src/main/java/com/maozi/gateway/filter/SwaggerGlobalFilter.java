package com.maozi.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.maozi.common.constant.SymbolConstant;
import jakarta.annotation.Nonnull;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Swagger/OpenAPI 文档聚合全局过滤器。
 * <p>
 * 拦截所有 /v3/api-docs 结尾的请求（即各微服务的 Swagger API 文档请求），
 * 对返回的 OpenAPI JSON 进行改写：
 * 1. 设置 host 为网关地址和端口；
 * 2. 设置 basePath 为对应微服务的路由前缀；
 * 3. 将所有 paths 中的路径添加微服务前缀，使前端 Swagger UI 通过网关访问各服务接口。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class SwaggerGlobalFilter implements GlobalFilter, Ordered {

    /** Swagger/OpenAPI 文档请求的路径后缀 */
    private static final String API_DOC_PATH = "/v3/api-docs";

    /** OpenAPI 文档 host 字段名 */
    private static final String HOST_KEY = "host";

    /** OpenAPI 文档 basePath 字段名 */
    private static final String BASE_PATH_KEY = "basePath";

    /** OpenAPI 文档 paths 字段名 */
    private static final String PATHS_KEY = "paths";

    /** 过滤器顺序：置于大多数默认过滤器之前 */
    private static final int FILTER_ORDER = -2;

    /** URL 路径分隔符切分模式（预编译） */
    private static final Pattern PATH_SPLIT_PATTERN = Pattern.compile(SymbolConstant.SLASH);

    /**
     * 过滤器核心逻辑。
     * <p>
     * 仅对路径以 /v3/api-docs 结尾的请求进行处理，其他请求直接放行。
     * 处理流程：
     * 1. 从请求路径中提取微服务标识（basePath）；
     * 2. 创建响应装饰器，拦截响应体；
     * 3. 解析原始 OpenAPI JSON，改写 host、basePath 和 paths；
     * 4. 返回改写后的 JSON 响应。
     * </p>
     *
     * @param exchange 服务端 Web 交换上下文
     * @param chain    网关过滤器链
     * @return 过滤器链执行结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        // 获取请求路径
        String path = request.getPath().toString();

        InetSocketAddress localAddress = request.getLocalAddress();

        // 获取网关主机地址
        String host = Objects.requireNonNull(localAddress).getHostString();
        // 获取网关端口
        int port = localAddress.getPort();

        // 仅处理 Swagger API 文档请求，其他请求直接放行
        if (!path.endsWith(API_DOC_PATH)) {
            return chain.filter(exchange);
        }

        // 从路径中提取微服务标识，如 /user-service/v3/api-docs -> user-service
        String basePath = PATH_SPLIT_PATTERN.split(path)[1];

        ServerHttpResponse originalResponse = exchange.getResponse();

        // 创建响应装饰器，拦截并改写 Swagger API 文档响应
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            /**
             * 拦截响应体写入，改写 OpenAPI JSON 内容。
             * <p>
             * 仅当响应状态为 200 且响应体为 Flux 时才进行改写，
             * 否则将原始响应体透传给下游处理。
             * </p>
             *
             * @param body 响应体数据流
             * @return 写入完成的 Void Mono
             */
            @Override
            public Mono<Void> writeWith(@Nonnull Publisher<? extends DataBuffer> body) {

                if (Objects.equals(super.getStatusCode(), HttpStatus.OK) && body instanceof Flux) {

                    Flux<DataBuffer> fluxBody = Flux.from(body);
                    return DataBufferUtils.join(fluxBody).flatMap(dataBuffer -> {

                        // 读取响应体字节数据
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        DataBufferUtils.release(dataBuffer);

                        // 改写 OpenAPI JSON；统一按 UTF-8 取字节数组，
                        // 保证 Content-Length 与响应体字节数一致
                        byte[] responseBytes = rewriteOpenApiDocument(content, host, port, basePath);
                        HttpHeaders headers = originalResponse.getHeaders();
                        headers.setContentLength(responseBytes.length);

                        // 将改写后的 JSON 包装为 DataBuffer 返回
                        DataBuffer buffer = super.bufferFactory().wrap(responseBytes);
                        return super.writeWith(Mono.just(buffer));

                    });
                }
                return super.writeWith(body);
            }

        };

        // 使用装饰器替换原始响应，继续执行过滤器链
        return chain.filter(exchange.mutate().response(decoratedResponse).build());

    }

    /**
     * 改写 OpenAPI 文档 JSON
     * <p>
     * 解析原始 JSON 后设置网关 host、微服务 basePath 前缀，
     * 并为所有 paths 中的路径添加微服务前缀，使前端 Swagger UI 通过网关访问各服务接口
     * </p>
     *
     * @param content 原始响应体字节数据
     * @param host 网关主机地址
     * @param port 网关端口
     * @param basePath 微服务路由前缀
     * @return 改写后文档的 UTF-8 字节数组
     */
    private byte[] rewriteOpenApiDocument(byte[] content, String host, int port, String basePath) {
        String response = new String(content, StandardCharsets.UTF_8);

        // 解析 JSON，禁用 fastjson 的特殊 key 检测（DisableSpecialKeyDetect），
        // 避免特殊 key 被特殊处理
        JSONObject jsonObject = JSON.parseObject(response, Feature.DisableSpecialKeyDetect);

        // 设置网关的 host 地址
        jsonObject.put(HOST_KEY, host + SymbolConstant.COLON + port);

        // 设置 basePath 为微服务路由前缀
        jsonObject.put(BASE_PATH_KEY, SymbolConstant.SLASH + basePath);

        // 获取原始 paths 对象
        JSONObject paths = (JSONObject)jsonObject.get(PATHS_KEY);
        JSONObject pathJsonObject = new JSONObject();
        Set<Entry<String, Object>> entries = paths.entrySet();

        // 为每个 API 路径添加微服务前缀（循环体内使用 StringBuilder 拼接）
        for (Entry<String, Object> entry : entries) {
            String key = new StringBuilder()
                    .append(SymbolConstant.SLASH).append(basePath).append(entry.getKey()).toString();
            pathJsonObject.put(key, entry.getValue());
        }

        // 替换原始 paths 为改写后的 paths
        jsonObject.put(PATHS_KEY, pathJsonObject);

        // 统一按 UTF-8 取字节数组，保证 Content-Length 与响应体字节数一致
        return JSON.toJSONString(jsonObject).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 获取过滤器执行顺序。
     * <p>
     * 设置为 -2，在大多数默认过滤器之前执行。
     * </p>
     *
     * @return 过滤器顺序值
     */
    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

}
