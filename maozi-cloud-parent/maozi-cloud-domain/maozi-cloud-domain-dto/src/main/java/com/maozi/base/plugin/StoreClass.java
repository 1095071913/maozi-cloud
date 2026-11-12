package com.maozi.base.plugin;

import com.maozi.base.enums.StoreClassType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储类注册中心
 * <p>
 * 按存储类型维护表名与实体 Class 的映射，供动态查询构建时按表名查找实体 Class。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class StoreClass {

    /** 存储类型 -> (表名 -> 实体Class对象) 的映射 */
    public static final Map<StoreClassType, Map<String, Class<?>>> storeClassMap = new ConcurrentHashMap<>();

}
