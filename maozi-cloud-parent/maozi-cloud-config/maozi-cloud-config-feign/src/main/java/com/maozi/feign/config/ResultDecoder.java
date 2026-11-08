package com.maozi.feign.config;

import com.maozi.common.JacksonUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.success.SuccessResult;
import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpMessageConverterExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feign 结果解码器
 * <p>
 * 自定义 Feign 响应解码逻辑，将远程调用的响应体反序列化为统一的结果对象。
 * 当返回类型为 {@link AbstractBaseResult} 时，封装为 {@link SuccessResult}；
 * 否则直接反序列化为目标类型。内置 Feign 响应到 Spring ClientHttpResponse 的适配器。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
public class ResultDecoder implements Decoder {

    /** HTTP 消息转换器工厂 */
    @Autowired
    private ObjectFactory<HttpMessageConverters> messageConverters;

    /**
     * 解码 Feign 响应
     * <p>
     * 要求目标返回类型为参数化类型（如 {@code Result<T>}），
     * 取其第一个泛型参数作为实际数据类型进行反序列化，非参数化类型会导致类型转换异常。
     * </p>
     *
     * @param response Feign 响应对象
     * @param type 目标返回类型（须为参数化类型）
     * @return 反序列化后的对象；返回类型包含 {@link AbstractBaseResult} 时封装为 {@link SuccessResult}
     * @throws IOException IO 异常
     * @throws FeignException Feign 异常
     */
    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {

        // 取目标返回类型的第一个泛型参数作为实际数据类型（如 Result<User> 中的 User）
        Type dataType = ((ParameterizedType)type).getActualTypeArguments()[0];

        // 基于 Spring 消息转换器构建提取器，借助 FeignResponseAdapter 将 Feign 响应适配为 ClientHttpResponse
        HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor<>(dataType, this.messageConverters.getObject().getConverters());

        // 提取响应体并解析为 Map 结构，供后续按目标类型二次转换
        LinkedHashMap<String, Object> extractData = (LinkedHashMap<String, Object>) extractor.extractData(new FeignResponseAdapter(response));

        // 返回类型包含 AbstractBaseResult 时封装为 SuccessResult，否则按实际数据类型转换
        return type.getTypeName().contains(AbstractBaseResult.class.getName()) ? JacksonUtil.mapToObject(extractData, SuccessResult.class) : JacksonUtil.mapToObject(extractData, dataType);

    }

    /**
     * Feign 响应到 Spring ClientHttpResponse 的适配器
     *
     * @author maozi
     */
    public static final class FeignResponseAdapter implements ClientHttpResponse {

        /** Feign 原始响应 */
        private final Response response;

        /**
         * 构造适配器
         *
         * @param response Feign 响应对象
         */
        private FeignResponseAdapter(Response response) {
            this.response = response;
        }

        /**
         * 获取 HTTP 状态码枚举
         *
         * @return Feign 响应状态码对应的 HttpStatus
         * @throws IOException 接口声明异常，本实现不会抛出
         */
        @NotNull
        @Override
        public HttpStatus getStatusCode() throws IOException {
            return HttpStatus.valueOf(this.response.status());
        }

        /**
         * 获取状态描述
         *
         * @return Feign 响应的状态描述文本
         * @throws IOException 接口声明异常，本实现不会抛出
         */
        @NotNull
        @Override
        public String getStatusText() throws IOException {
            return this.response.reason();
        }

        /**
         * 关闭响应体
         */
        @Override
        public void close() {
            try {
                this.response.body().close();
            }
            catch (IOException ex) {
                // 关闭响应体时的异常可忽略，不影响主流程
            }
        }

        /**
         * 获取响应体输入流
         *
         * @return Feign 响应体对应的输入流
         * @throws IOException 打开响应体流失败时抛出
         */
        @NotNull
        @Override
        public InputStream getBody() throws IOException {
            return this.response.body().asInputStream();
        }

        /**
         * 获取响应头
         *
         * @return 转换为 Spring HttpHeaders 的响应头
         */
        @NotNull
        @Override
        public HttpHeaders getHeaders() {
            return getHttpHeaders(this.response.headers());
        }

    }

    /**
     * 将 Feign 响应头转换为 Spring HttpHeaders
     *
     * @param headers Feign 响应头映射
     * @return Spring HttpHeaders 对象
     */
    static HttpHeaders getHttpHeaders(Map<String, Collection<String>> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        // 逐项复制 Feign 响应头到 Spring HttpHeaders
        for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
            httpHeaders.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return httpHeaders;
    }

}
