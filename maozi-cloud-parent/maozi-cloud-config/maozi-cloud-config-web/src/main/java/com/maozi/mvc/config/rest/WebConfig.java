package com.maozi.mvc.config.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maozi.mvc.config.json.ReadOnlyMultipartFormDataEndpointConverter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Web MVC 配置
 * <p>
 * 注册自定义消息转换器：
 * 添加 {@link ReadOnlyMultipartFormDataEndpointConverter} 支持 multipart/form-data 请求体读取。
 * 当前用户信息拦截器不在此注册，由 {@code maozi-cloud-config-oauth} 模块的 {@code WebSecurityConfig} 负责
 * （版本号、traceId 等链路上下文由本模块的 {@code ApplicationContextFilter} 以 Servlet Filter 方式处理）。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Jackson ObjectMapper 实例 */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 扩展消息转换器列表
     * <p>
     * 添加支持 {@code multipart/form-data} 和 {@code application/octet-stream} 的
     * 只读 Jackson 消息转换器。
     * </p>
     *
     * @param converters 消息转换器列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

        // 创建支持 multipart/form-data 的只读 Jackson 消息转换器
        ReadOnlyMultipartFormDataEndpointConverter converter = new ReadOnlyMultipartFormDataEndpointConverter(objectMapper);

        // 在默认支持的媒体类型基础上，额外添加 application/octet-stream 支持
        List<MediaType> supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        converter.setSupportedMediaTypes(supportedMediaTypes);

        // 将自定义转换器添加到转换器列表中
        converters.add(converter);

    }

}
