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
package com.alibaba.csp.sentinel.dashboard.util;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.function.Tuple2;

import java.util.Optional;

/**
 * 解析 {@code ip@commandPort} 形式机器 ID 的工具类
 *
 * @author Eric Zhao
 * @date 2026/11/22
 */
public final class MachineUtils {

    /** 机器 ID 中 ip 与命令端口之间的分隔符 */
    public static final String MACHINE_ID_SEPARATOR = "@";

    /**
     * 从 {@code ip@commandPort} 形式的机器 ID 解析命令端口，
     * ID 无端口或无法解析时返回 empty
     */
    public static Optional<Integer> parseCommandPort(String machineIp) {
        return parseCommandIpAndPort(machineIp).map(ipPort -> ipPort.r2);
    }

    /**
     * 从 {@code ip@commandPort} 形式的机器 ID 解析 ip 与命令端口，
     * ID 为空、无端口或无法解析时返回 empty
     */
    public static Optional<Tuple2<String, Integer>> parseCommandIpAndPort(String machineIp) {
        try {
            if (StringUtil.isEmpty(machineIp) || !machineIp.contains(MACHINE_ID_SEPARATOR)) {
                return Optional.empty();
            }
            String[] segments = machineIp.split(MACHINE_ID_SEPARATOR);
            if (segments.length <= 1) {
                return Optional.empty();
            }
            return Optional.of(Tuple2.of(segments[0], Integer.parseInt(segments[1])));
        } catch (Exception ex) {
            // 机器 ID 格式非法（如端口非数字）时视为无法解析
            return Optional.empty();
        }
    }

    /** 私有构造方法，防止实例化 */
    private MachineUtils() {}
}
