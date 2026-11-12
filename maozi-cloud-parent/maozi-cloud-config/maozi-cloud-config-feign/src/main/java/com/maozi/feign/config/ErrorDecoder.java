package com.maozi.feign.config;

import com.maozi.common.result.error.exception.BusinessResultException;
import feign.Response;
import feign.Util;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Feign 错误解码器
 * <p>
 * 当 Feign 远程调用返回非 2xx 状态码时，将响应体解析为 {@link BusinessResultException}，
 * 保留原始 HTTP 状态码和错误信息。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class ErrorDecoder implements feign.codec.ErrorDecoder {

    /**
     * 解码错误响应
     *
     * @param methodKey Feign 方法键
     * @param response HTTP 响应对象
     * @return 响应体解析成功时返回携带原始 HTTP 状态码和响应体内容的 {@link BusinessResultException}；
     *         读取响应体失败时返回原始 {@link IOException}
     */
    @Override
    public Exception decode(String methodKey, Response response) {

        try {return new BusinessResultException(response.status(),Util.toString(response.body().asReader(Charset.defaultCharset()))).setHttpCode(response.status());} catch (IOException e) {
            return e;
        }

    }

}
