package com.maozi.mvc.config.rest;

import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.context.SqlLogContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.enums.EnvironmentTypeEnum;
import com.maozi.common.enums.LogCommonTypeEnum;
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
import java.nio.charset.StandardCharsets;
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
     * @param url 请求 URL，不能为空
     * @param uriTemplate URI 模板（本实现未使用，仅为匹配父类方法签名）
     * @param method HTTP 方法，不能为空
     * @param requestCallback 请求回调，用于写入请求头和请求体，可为空
     * @param responseExtractor 响应提取器，用于反序列化响应体，可为空
     * @param <T> 响应数据类型
     * @return 响应提取器解析出的数据，未提供响应提取器时返回 null
     * @throws RestClientException RestClientException 原样上抛，
     *     其他异常记录日志后包装为 RestClientException 抛出
     */
    @Nullable
    @Override
    protected <T> T doExecute(@Nonnull URI url, @Nullable String uriTemplate, @Nullable HttpMethod method,
        @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor)
        throws RestClientException {

        // 标记请求是否发生异常，用于最终日志级别判断
        boolean error = false;

        // 捕获的异常引用，供 finally 中合并输出结构化日志与异常堆栈
        Throwable ex = null;

        // 使用有序 Map 收集请求全链路日志信息
        // （后续固定写入 IP/TYPE/URL/METHOD 等标签，指定初始容量）
        Map<String, String> logs = new LinkedHashMap<>(16);


        // HTTP 响应对象，用于 finally 中统一关闭
        ClientHttpRequest request = null;
        ClientHttpResponse response = null;
        try {

            // 校验 URL 和 HTTP 方法不能为空
            Assert.notNull(url, "URI is required");
            Assert.notNull(method, "HttpMethod is required");

            // 创建 HTTP 请求对象
            request = super.createRequest(url, method);

            // 记录请求类型、URI 和 HTTP 方法到日志
            logs.put(LogTag.TYPE, LogCommonTypeEnum.REST_TEMPLATE.getDesc());
            logs.put(LogTag.URL, request.getURI().toString());
            logs.put(LogTag.METHOD, request.getMethod().toString());

            // 执行请求回调，设置请求头和请求体
            if (requestCallback != null) {
                requestCallback.doWithRequest(request);
            }

            // 执行 HTTP 请求并获取响应
            response = request.execute();

            // 处理响应状态码，错误状态码（4xx/5xx）时抛出异常
            super.handleResponse(url, method, response);

            // 使用响应提取器将响应体反序列化为目标类型
            T data = (responseExtractor != null ? responseExtractor.extractData(response) : null);

            // 记录响应状态码和响应数据到日志
            logs.put(LogTag.CODE, Integer.toString(response.getStatusCode().value()));
            if (ObjectUtil.isNotNull(data)) {
                logs.put(LogTag.DATA, data.toString());
            }

            // 从请求输出流中读取请求参数内容，用于日志记录
            // 非生产环境才记录请求参数，避免生产环境敏感信息写入日志
            if (EnvironmentUtil.notEnvironment(EnvironmentTypeEnum.PROD)) {
                InputStream paramData = parse(request.getBody());
                String requestParam = new String(readStream(paramData), StandardCharsets.UTF_8);
                logs.put(LogTag.PARAM, requestParam);
            }

            return data;

        } catch (Exception e) {

            // 标记为异常状态
            error = true;

            // 暂存异常引用，堆栈在 finally 中随结构化日志合并输出，避免同一异常重复打印
            ex = e;

            // 记录异常相关的用户、描述和堆栈位置到日志
            Long userId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUserId);
            if (ObjectUtil.isNotNull(userId)) {
                logs.put(LogTag.ERROR_USER, userId.toString());
            }

            if(ObjectUtil.isNotNull(request)){
                try {
                    OutputStream body = request.getBody();
                    InputStream paramData = parse(body);
                    String requestParam = new String(readStream(paramData), StandardCharsets.UTF_8);
                    logs.put(LogTag.PARAM, requestParam);
                } catch (Exception exc) {
                    // 辅助读取请求参数失败不覆盖原始异常，追加为被抑制异常保留原因链
                    ex.addSuppressed(exc);
                }
            }
            
            // RestClientException 原样上抛，其他异常包装为 RestClientException
            throw (ex instanceof RestClientException restClientException) ? restClientException
                : new RestClientException(ex.getMessage(), ex);

        } finally {

            // 确保响应流被关闭，防止资源泄漏
            if (response != null) {
                response.close();
            }

            // 收集本次请求链路中累积的 SQL 日志
            StringBuilder sqlLog = SqlLogContext.CONTEXT.get();
            if (ObjectUtil.isNotNull(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            // 根据是否有异常，以对应日志级别输出完整的请求日志
            // （异常时结构化字段与异常堆栈合并为一条日志，避免重复打印）
            LogUtil.log(log, error, logs, ex);

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
     * @param inStream 输入流，读取完成后会被关闭
     * @return 包含流中全部数据的字节数组
     * @throws Exception 流读取异常
     */
    public byte[] readStream(InputStream inStream) throws Exception {
        try (inStream; ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            return outStream.toByteArray();
        }
    }

}
