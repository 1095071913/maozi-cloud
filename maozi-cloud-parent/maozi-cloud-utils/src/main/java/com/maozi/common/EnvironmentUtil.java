package com.maozi.common;

import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.enums.EnvironmentTypeEnum;

import java.util.Objects;

/**
 * 环境工具类
 * <p>
 * 基于 {@link ApplicationEnvironmentContext} 中存储的环境标识
 * 与 {@link EnvironmentTypeEnum} 枚举描述值比对，判断当前运行环境类型。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/4/26 09:27
 * @see EnvironmentTypeEnum 环境类型枚举
 * @see ApplicationEnvironmentContext 应用环境上下文
 */
public class EnvironmentUtil {

    /**
     * 判断当前环境是否为指定类型
     *
     * @param type 目标环境类型
     * @return 一致时返回 true，否则返回 false
     */
    public static boolean isEnvironment(EnvironmentTypeEnum type) {
        // 环境标识由静态上下文运行时注入（可能为 null），使用 Objects.equals 防止 NPE
        return Objects.equals(ApplicationEnvironmentContext.environment, type.getDesc());
    }

    /**
     * 判断当前环境是否不为指定类型
     *
     * @param type 目标环境类型
     * @return 不一致时返回 true，一致时返回 false
     */
    public static boolean notEnvironment(EnvironmentTypeEnum type) {
        return !isEnvironment(type);
    }

}
