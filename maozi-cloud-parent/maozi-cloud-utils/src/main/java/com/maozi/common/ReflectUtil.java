package com.maozi.common;

import cn.hutool.core.util.StrUtil;

/**
 * 反射工具类
 * <p>
 * 扩展 Hutool 的 {@link cn.hutool.core.util.ReflectUtil}，
 * 提供更简洁的 getter/setter 方法调用和名称生成工具。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class ReflectUtil extends cn.hutool.core.util.ReflectUtil {

    /** getter 方法前缀 */
    public static final String GET_FUN_PREFIX_NAME = "get";

    /** setter 方法前缀 */
    public static final String SET_FUN_PREFIX_NAME = "set";

    /** getId 方法名 */
    public static final String GET_ID_FUN_NAME = "getId";

    /**
     * 通过反射调用对象的 getter 方法获取字段值
     *
     * @param obj 目标对象
     * @param fieldName 字段名
     * @param <R> 返回值类型
     * @return 字段值
     */
    public static <R> R invokeGet(Object obj, String fieldName) {
        return cn.hutool.core.util.ReflectUtil.invoke(obj, getter(fieldName));
    }

    /**
     * 通过反射调用对象的 setter 方法设置字段值
     *
     * @param obj 目标对象
     * @param fieldName 字段名
     * @param value 设置的值
     */
    public static void invokeSet(Object obj, String fieldName, Object value) {
        cn.hutool.core.util.ReflectUtil.invoke(obj, setter(fieldName), value);
    }

    /**
     * 通过反射调用对象的 getId 方法获取主键值
     *
     * @param obj 目标对象
     * @param <R> 返回值类型
     * @return 主键值
     */
    public static <R> R invokeFunGetId(Object obj){
        return cn.hutool.core.util.ReflectUtil.invoke(obj, GET_ID_FUN_NAME);
    }

    /**
     * 根据字段名生成 getter 方法名
     *
     * @param name 字段名
     * @return getter 方法名
     */
    public static String getter(String name) {
        return GET_FUN_PREFIX_NAME + StrUtil.upperFirst(name);
    }

    /**
     * 根据字段名生成 setter 方法名
     *
     * @param name 字段名
     * @return setter 方法名
     */
    public static String setter(String name) {
        return SET_FUN_PREFIX_NAME + StrUtil.upperFirst(name);
    }

}
