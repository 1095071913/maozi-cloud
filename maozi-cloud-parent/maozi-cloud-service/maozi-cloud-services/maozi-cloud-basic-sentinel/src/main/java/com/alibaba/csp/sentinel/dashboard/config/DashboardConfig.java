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
 * <p>Dashboard local config support.</p>
 * <p>
 * Dashboard supports configuration loading by several ways by order:<br>
 * 1. Env<br>
 * 2. System properties
 * </p>
 *
 * @author jason
 * @since 1.5.0
 */
public class DashboardConfig {

    /** Default timeout in millisecond to regard a machine as unhealthy. */
    public static final int DEFAULT_MACHINE_HEALTHY_TIMEOUT_MS = 60_000;

    /**
     * Login username
     */
    public static final String CONFIG_AUTH_USERNAME = "sentinel.dashboard.auth.username";

    /**
     * Login password
     */
    public static final String CONFIG_AUTH_PASSWORD = "sentinel.dashboard.auth.password";

    /**
     * Hide application name in sidebar when it has no healthy machines after specific period in millisecond.
     */
    public static final String CONFIG_HIDE_APP_NO_MACHINE_MILLIS = "sentinel.dashboard.app.hideAppNoMachineMillis";
    /**
     * Remove application when it has no healthy machines after specific period in millisecond.
     */
    public static final String CONFIG_REMOVE_APP_NO_MACHINE_MILLIS = "sentinel.dashboard.removeAppNoMachineMillis";
    /**
     * Timeout in millisecond to regard a machine as unhealthy.
     */
    public static final String CONFIG_UNHEALTHY_MACHINE_MILLIS = "sentinel.dashboard.unhealthyMachineMillis";

    /** Cache of resolved config values, keyed by config name. */
    private static final ConcurrentMap<String, Object> cacheMap = new ConcurrentHashMap<>();
    
    /** Returns the raw config value, checking env first and then system properties; empty string if unset. */
    @NonNull
    private static String getConfig(String name) {
        // env
        String val = System.getenv(name);
        if (StringUtils.isNotEmpty(val)) {
            return val;
        }
        // properties
        val = System.getProperty(name);
        if (StringUtils.isNotEmpty(val)) {
            return val;
        }
        return "";
    }

    /** Returns the cached config string value, or null if unset or blank (blank values are not cached). */
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
     * Returns the cached config int value: an unparsable or zero value falls back to
     * the default, and values below minVal are raised to minVal.
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

    /** Get the configured login username, or null if unset. */
    public static String getAuthUsername() {
        return getConfigStr(CONFIG_AUTH_USERNAME);
    }

    /** Get the configured login password, or null if unset. */
    public static String getAuthPassword() {
        return getConfigStr(CONFIG_AUTH_PASSWORD);
    }

    /** Get the hide-app-no-machine period in millisecond (default 0, min 60000). */
    public static int getHideAppNoMachineMillis() {
        return getConfigInt(CONFIG_HIDE_APP_NO_MACHINE_MILLIS, 0, 60000);
    }
    
    /** Get the remove-app-no-machine period in millisecond (default 0, min 120000). */
    public static int getRemoveAppNoMachineMillis() {
        return getConfigInt(CONFIG_REMOVE_APP_NO_MACHINE_MILLIS, 0, 120000);
    }

    /** Get the unhealthy machine timeout in millisecond (default 60000, min 30000). */
    public static int getUnhealthyMachineMillis() {
        return getConfigInt(CONFIG_UNHEALTHY_MACHINE_MILLIS, DEFAULT_MACHINE_HEALTHY_TIMEOUT_MS, 30000);
    }
    
    /** Clear the cache of resolved config values. */
    public static void clearCache() {
        cacheMap.clear();
    }
}
