package com.maozi.service.api.annotation;

import jakarta.annotation.Resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程资源消费者注解
 * <p>
 * 标注在非静态字段上，表示需要注入一个 Dubbo RPC 远程资源（消费端）。
 * 该注解组合了 {@link Resource}，可被以下 Bean 后置处理器识别并完成注入：
 * <ul>
 *   <li>分布式部署：{@code RemoteResourceBeanPostProcessor}（从 Dubbo 注册中心获取代理）</li>
 *   <li>单体部署：{@code LocalRemoteResourceBeanPostProcessor}（直接从 Spring 容器取本地 Bean）</li>
 * </ul>
 * 与 {@link RemoteService}（生产端注解）成对使用。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/25 15:12
 * @see RemoteService
 */
@Documented
@Resource
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface RemoteResource {
}
