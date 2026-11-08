package com.maozi.service.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程服务生产者注解
 * <p>
 * 标注在类或方法上，表示当前类是一个 Dubbo RPC 远程服务的生产端实现。
 * 按部署模式由以下组件识别并注册：
 * <ul>
 *   <li>分布式部署：{@code RemoteServiceBeanPostProcessor}（注册到 Dubbo 注册中心对外暴露）</li>
 *   <li>单体部署：{@code LocalRemoteServiceRegistrar}（注册为本地 Spring Bean，供 {@link RemoteResource} 直接注入）</li>
 * </ul>
 * 与 {@link RemoteResource}（消费端注解）成对使用。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/25 15:12
 * @see RemoteResource
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RemoteService {
}
