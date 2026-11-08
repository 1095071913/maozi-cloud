package com.maozi.service.annotation;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GET 请求映射注解
 * <p>
 * 组合了 {@link RequestMapping}（GET 方法）和 {@link Operation}（Swagger 文档）注解，
 * 简化 GET 接口的定义。通过 {@code description} 属性同时配置接口描述和 Swagger 摘要。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Operation
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(method = {RequestMethod.GET})
public @interface Get {

    /** 请求路径 */
    @AliasFor(annotation = RequestMapping.class)
    String[] value() default {};

    /** 接口描述，同时作为 Swagger 文档的 summary */
    @AliasFor(value = "summary",annotation = Operation.class)
    String description() default "";

}
