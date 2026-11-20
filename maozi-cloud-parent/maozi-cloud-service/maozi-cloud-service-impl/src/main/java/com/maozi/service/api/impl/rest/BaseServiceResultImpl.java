package com.maozi.service.api.impl.rest;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST 服务结果实现基类
 * <p>
 * 持有一个自动注入的通用业务服务实例，供各业务 REST 层继承复用。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/11/19 14:42
 */
public class BaseServiceResultImpl<S> {

    /**
     * 关联的通用业务服务
     * <p>
     * 保留字段注入的依据：本基类的继承层级较深（BaseDBTServiceResultImpl、
     * BaseDBServiceResultImpl 及十余个业务实现类），而 Lombok 的
     * {@code @RequiredArgsConstructor} 无法为继承字段生成构造器，
     * 也不支持在子类中透传 {@code super(...)}，改构造器注入需全链路
     * 显式声明构造器，侵入面过大，权衡后维持字段注入。
     * </p>
     */
    @SuppressWarnings("java:S6813")
    @Autowired
    protected S service;
    
}
