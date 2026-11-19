package com.maozi.service.api.impl.rest;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author pengjinlong
 * @date 2026/11/19 14:42
 */
public class BaseServiceResultImpl<S> {

    /** 关联的通用业务服务 */
    @Autowired
    protected S service;
    
}
