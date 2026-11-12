package com.maozi.mvc.config.rest;

import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.common.enums.LogCommonType;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义 RestTemplate
 * <p>
 * 扩展 Spring 的 {@link org.springframework.web.client.RestTemplate}，
 * 在 {@link #doExecute} 方法中集成请求日志记录、响应时间统计、
 * SQL 日志收集和异常处理。所有 HTTP 请求（GET/POST/HEAD/EXCHANGE 等）
 * 最终均经由基类公开方法内部调用的 {@link #doExecute} 统一走增强后的流程。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
public class RestTemplate extends org.springframework.web.client.RestTemplate {

    /** 流读取缓冲区大小（字节） */
    private static final int BUFFER_SIZE = 1024;

    /**
     * 执行 HTTP 请求的核心方法（增强版）
     * <p>
     * 在原始执行逻辑基础上添加了请求日志、响应日志、异常日志、
     * SQL 日志收集。
     * </p>
     *
     * @param url 请求 URL
     * @param method HTTP 方法
     * @param requestCallback 请求回调
     * @param responseExtractor 响应提取器
     * @return 响应数据
     * @throws RestClientException 仅在方法签名上声明；
     *     实现中所有异常均被捕获并记录日志后返回 null，不会向上抛出
     */
    @Nullable
    @Override
    protected <T> T doExecute(@Nonnull URI url, @Nullable String uriTemplate, @Nullable HttpMethod method,
        @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor)
        throws RestClientException {

        // 标记请求是否发生异常，用于最终日志级别判断
        boolean error = false;

        // 记录请求开始时间，用于计算响应耗时
        long startTime = System.currentTimeMillis();

        // 使用有序 Map 收集请求全链路日志信息
        Map<String, String> logs = new LinkedHashMap<>();

        ClientHttpResponse response = null;

        try {

            // 校验 URL 和 HTTP 方法不能为空
            Assert.notNull(url, "URI is required");
            Assert.notNull(method, "HttpMethod is required");

            // 创建 HTTP 请求对象
            ClientHttpRequest request = createRequest(url, method);

            // 记录请求类型、URI 和 HTTP 方法到日志
            logs.put(LogTag.TYPE, LogCommonType.REST_TEMPLATE.getDesc());
            logs.put(LogTag.URL, request.getURI().toString());
            logs.put(LogTag.METHOD, request.getMethod().toString());

            // 执行请求回调，设置请求头和请求体
            if (requestCallback != null) {
                requestCallback.doWithRequest(request);
            }

            // 从请求输出流中读取请求参数内容，用于日志记录
            InputStream paramData = parse(request.getBody());
            String requestParam = new String(readStream(paramData));

            // 非生产环境才记录请求参数，避免生产环境敏感信息写入日志
            boolean isNotProd = EnvironmentUtil.notEnvironment(EnvironmentType.PROD);
            if (isNotProd) {
                logs.put(LogTag.PARAM, requestParam);
            }

            // 执行 HTTP 请求并获取响应
            response = request.execute();

            // 处理响应状态码，错误状态码（4xx/5xx）时抛出异常
            handleResponse(url, method, response);

            // 使用响应提取器将响应体反序列化为目标类型
            T data = (responseExtractor != null ? responseExtractor.extractData(response) : null);

            // 记录响应状态码、响应耗时和响应数据到日志
            logs.put(LogTag.CODE, Integer.toString(response.getStatusCode().value()));
            if (ObjectUtil.isNotNullEmpty(data)) {
                logs.put(LogTag.DATA, data.toString());
            }

            return data;

        } catch (Exception e) {

            // 标记为异常状态
            error = true;

            log.error("", e);

            // 记录异常相关的用户、描述和堆栈位置到日志
            Long userId = ApplicationLinkContext.getCurrentUserInfo(CurrentUserInfo::getUserId);
            if (ObjectUtil.isNotNullEmpty(userId)) {
                logs.put(LogTag.ERROR_USER, userId.toString());
            }
            logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());
            logs.put(LogTag.ERROR_LINE, e.getStackTrace()[0].toString());

            throw (e instanceof RestClientException restClientException) ? restClientException
                : new RestClientException(e.getMessage(), e);

        } finally {

            // 确保响应流被关闭，防止资源泄漏
            if (response != null) {
                response.close();
            }

            // 收集本次请求链路中累积的 SQL 日志
            StringBuilder sqlLog = LogUtil.SQL_LOG.get();
            if (ObjectUtil.isNotNullEmpty(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            // 最终记录响应耗时（包含异常情况下的耗时）
            logs.put(LogTag.RT, (System.currentTimeMillis() - startTime) + " ms");

            // 根据是否有异常，以对应日志级别输出完整的请求日志
            LogUtil.log(log, error, logs);

        }

    }

    /**
     * 将输出流转换为输入流（用于读取请求体内容）
     * <p>
     * 将 ByteArrayOutputStream 中的字节数据包装为 ByteArrayInputStream，
     * 以便在日志中记录请求参数。
     * </p>
     *
     * @param out 输出流（实际类型为 ByteArrayOutputStream）
     * @return 包含相同字节数据的输入流
     */
    public ByteArrayInputStream parse(final OutputStream out) {
        ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) out;
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    /**
     * 读取输入流中的全部字节数据
     * <p>
     * 使用 1024 字节的缓冲区逐块读取输入流，直到流结束，
     * 将所有数据合并为字节数组返回。用于从请求体流中提取参数内容。
     * </p>
     *
     * @param inStream 输入流
     * @return 包含流中全部数据的字节数组
     * @throws Exception 流读取异常
     */
    public byte[] readStream(InputStream inStream) throws Exception {
        try (inStream; ByteArrayOutputStream outSteam = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            return outSteam.toByteArray();
        }
    }

}
