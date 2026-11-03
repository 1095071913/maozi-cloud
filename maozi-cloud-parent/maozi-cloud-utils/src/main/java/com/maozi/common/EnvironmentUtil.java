package com.maozi.common;

import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.enums.EnvironmentType;

/**
 * 环境工具类
 * <p>
 * 基于 {@link ApplicationEnvironmentContext} 中存储的环境标识
 * 与 {@link EnvironmentType} 枚举描述值比对，判断当前运行环境类型。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/4/26 09:27
 * @see EnvironmentType 环境类型枚举
 * @see ApplicationEnvironmentContext 应用环境上下文
 */
public class EnvironmentUtil {

    /**
     * 判断当前环境是否为指定类型
     *
     * @param type 目标环境类型
     * @return 一致时返回 true，否则返回 false
     */
    public static Boolean isEnvironment(EnvironmentType type) {
        return type.getDesc().equals(ApplicationEnvironmentContext.ENVIRONMENT);
    }

    /**
     * 判断当前环境是否不为指定类型
     *
     * @param type 目标环境类型
     * @return 不一致时返回 true，一致时返回 false
     */
    public static Boolean notEnvironment(EnvironmentType type) {
        return !type.getDesc().equals(ApplicationEnvironmentContext.ENVIRONMENT);
    }

}
