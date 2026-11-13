package com.maozi.mvc.config.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Type;

/**
 * 只读 Multipart 表单数据转换器
 * <p>
 * 扩展 {@link MappingJackson2HttpMessageConverter}，仅当控制器方法
 * 的 {@code @RequestMapping} 注解明确声明消费 {@code multipart/form-data} 时，
 * 才使用 Jackson 将请求体反序列化为对象。仅支持读取，不支持写入。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class ReadOnlyMultipartFormDataEndpointConverter extends MappingJackson2HttpMessageConverter {

    /**
     * 基于指定的 ObjectMapper 构造转换器
     *
     * @param objectMapper Jackson ObjectMapper 实例
     */
    public ReadOnlyMultipartFormDataEndpointConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 判断是否可以读取指定的媒体类型
     * <p>
     * 仅在以下条件全部满足时才继续委托父类判断，任一不满足直接返回 false：
     * <ol>
     *   <li>当前存在 HTTP 请求上下文</li>
     *   <li>能获取到处理方法信息</li>
     *   <li>处理方法标注了 {@code @RequestMapping} 注解</li>
     *   <li>{@code @RequestMapping} 的 consumes 仅声明了 multipart/form-data</li>
     * </ol>
     * </p>
     *
     * @param type 目标类型
     * @param contextClass 上下文类
     * @param mediaType 媒体类型
     * @return 上述条件均满足且父类判定可读时为 true，否则为 false
     */
    @Override
    public boolean canRead(@Nonnull Type type, Class<?> contextClass, MediaType mediaType) {
        // 当使用 REST 客户端（如 RestTemplate#getForObject）发请求时，RequestAttributes 可能为 null
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return false;
        }
        // 从请求上下文中获取当前匹配的处理器方法
        HandlerMethod handlerMethod = (HandlerMethod) requestAttributes.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (handlerMethod == null) {
            return false;
        }
        // 获取处理器方法上的 @RequestMapping 注解（含 @PostMapping 等组合注解）
        RequestMapping requestMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return false;
        }
        // 仅当 @RequestMapping 的 consumes 属性只声明了 multipart/form-data 时才启用此转换器，
        // 避免对所有 multipart 请求都进行 JSON 反序列化
        if (requestMapping.consumes().length != 1|| !MediaType.MULTIPART_FORM_DATA_VALUE.equals(requestMapping.consumes()[0])) {
            return false;
        }
        return super.canRead(type, contextClass, mediaType);
    }

    /**
     * 此转换器仅用于请求读取，不支持写入响应
     *
     * @param mediaType 媒体类型
     * @return 恒为 false，不支持写入
     */
    @Override
    protected boolean canWrite(MediaType mediaType) {
        return false;
    }
}
