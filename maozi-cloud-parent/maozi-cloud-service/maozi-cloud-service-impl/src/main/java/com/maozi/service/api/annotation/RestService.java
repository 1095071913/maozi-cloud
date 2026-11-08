package com.maozi.service.api.annotation;

import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * REST 服务注解
 * <p>
 * 组合了 {@link RestController} 注解，标注在类上表示该类是一个 REST 控制器。
 * 用于替代直接使用 {@code @RestController}，统一项目中的 REST 接口标识。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Documented
@RestController
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestService {}
