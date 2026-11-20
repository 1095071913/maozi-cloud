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

    /** 关联的通用业务服务 */
    @Autowired
    protected S service;
    
}
