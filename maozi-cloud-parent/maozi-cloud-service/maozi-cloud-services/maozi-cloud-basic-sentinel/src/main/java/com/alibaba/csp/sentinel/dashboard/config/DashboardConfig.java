/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.config;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.lang.NonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 控制台本地配置支持：优先从环境变量、其次从系统属性解析配置值，并缓存结果。
 *
 * @author jason
 * @date 2026/11/22
 * @since 1.5.0
 */
public class DashboardConfig {

    /** 判定机器不健康的默认超时时间（毫秒）。 */
    public static final int DEFAULT_MACHINE_HEALTHY_TIMEOUT_MS = 60_000;
    /** 在侧边栏隐藏无健康机器应用的最小周期（毫秒）。 */
    public static final int MIN_HIDE_APP_NO_MACHINE_MS = 60_000;
    /** 移除无健康机器应用的最小周期（毫秒）。 */
    public static final int MIN_REMOVE_APP_NO_MACHINE_MS = 120_000;
    /** 判定机器不健康的最小超时时间（毫秒）。 */
    public static final int MIN_UNHEALTHY_MACHINE_MS = 30_000;
    /** 周期类配置的禁用标记（0 表示禁用该功能）。 */
    public static final int DISABLED_PERIOD_MS = 0;

    /** 登录用户名的配置键。 */
    public static final String CONFIG_AUTH_USERNAME = "sentinel.dashboard.auth.username";

    /** 登录密码的配置键。 */
    public static final String CONFIG_AUTH_PASSWORD = "sentinel.dashboard.auth.password";

    /** 无健康机器应用在侧边栏隐藏周期的配置键（毫秒）。 */
    public static final String CONFIG_HIDE_APP_NO_MACHINE_MILLIS = "sentinel.dashboard.app.hideAppNoMachineMillis";
    /** 无健康机器应用移除周期的配置键（毫秒）。 */
    public static final String CONFIG_REMOVE_APP_NO_MACHINE_MILLIS = "sentinel.dashboard.removeAppNoMachineMillis";
    /** 机器不健康超时时间的配置键（毫秒）。 */
    public static final String CONFIG_UNHEALTHY_MACHINE_MILLIS = "sentinel.dashboard.unhealthyMachineMillis";

    /** 已解析配置值的缓存，键为配置名。 */
    private static final ConcurrentMap<String, Object> cacheMap = new ConcurrentHashMap<>();
    
    /**
     * 获取原始配置值：先查环境变量，再查系统属性，均未设置时返回空字符串。
     *
     * @param name 配置键
     * @return 配置值
     */
    @NonNull
    private static String getConfig(String name) {
        // 先查环境变量
        String val = System.getenv(name);
        if (StringUtils.isNotEmpty(val)) {
            return val;
        }
        // 再查系统属性
        val = System.getProperty(name);
        if (StringUtils.isNotEmpty(val)) {
            return val;
        }
        return "";
    }

    /**
     * 获取带缓存的字符串配置值，未设置或为空白时返回 null（空白值不缓存）。
     *
     * @param name 配置键
     * @return 配置值，未设置时返回 null
     */
    protected static String getConfigStr(String name) {
        if (cacheMap.containsKey(name)) {
            return (String) cacheMap.get(name);
        }

        String val = getConfig(name);

        if (StringUtils.isBlank(val)) {
            return null;
        }

        cacheMap.put(name, val);
        return val;
    }

    /**
     * 获取带缓存的整型配置值：无法解析或为 0 时回退默认值，低于最小值时提升为最小值。
     *
     * @param name      配置键
     * @param defaultVal 默认值
     * @param minVal    最小值
     * @return 配置值
     */
    protected static int getConfigInt(String name, int defaultVal, int minVal) {
        if (cacheMap.containsKey(name)) {
            return (int)cacheMap.get(name);
        }
        int val = NumberUtils.toInt(getConfig(name));
        if (val == 0) {
            val = defaultVal;
        } else if (val < minVal) {
            val = minVal;
        }
        cacheMap.put(name, val);
        return val;
    }

    /**
     * 获取配置的登录用户名。
     *
     * @return 登录用户名，未设置时返回 null
     */
    public static String getAuthUsername() {
        return getConfigStr(CONFIG_AUTH_USERNAME);
    }

    /**
     * 获取配置的登录密码。
     *
     * @return 登录密码，未设置时返回 null
     */
    public static String getAuthPassword() {
        return getConfigStr(CONFIG_AUTH_PASSWORD);
    }

    /**
     * 获取无健康机器应用的侧边栏隐藏周期（毫秒，默认 0 即禁用，最小 60000）。
     *
     * @return 隐藏周期（毫秒）
     */
    public static int getHideAppNoMachineMillis() {
        return getConfigInt(CONFIG_HIDE_APP_NO_MACHINE_MILLIS, DISABLED_PERIOD_MS, MIN_HIDE_APP_NO_MACHINE_MS);
    }

    /**
     * 获取无健康机器应用的移除周期（毫秒，默认 0 即禁用，最小 120000）。
     *
     * @return 移除周期（毫秒）
     */
    public static int getRemoveAppNoMachineMillis() {
        return getConfigInt(CONFIG_REMOVE_APP_NO_MACHINE_MILLIS, DISABLED_PERIOD_MS, MIN_REMOVE_APP_NO_MACHINE_MS);
    }

    /**
     * 获取机器不健康的判定超时时间（毫秒，默认 60000，最小 30000）。
     *
     * @return 超时时间（毫秒）
     */
    public static int getUnhealthyMachineMillis() {
        return getConfigInt(CONFIG_UNHEALTHY_MACHINE_MILLIS, DEFAULT_MACHINE_HEALTHY_TIMEOUT_MS, MIN_UNHEALTHY_MACHINE_MS);
    }
    
    /** 清空已解析配置值的缓存。 */
    public static void clearCache() {
        cacheMap.clear();
    }
}
