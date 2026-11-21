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

import com.alibaba.csp.sentinel.dashboard.datasource.entity.SentinelVersion;
import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.Optional;

/**
 * 解析 Sentinel 版本字符串的工具类
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
public final class VersionUtils {

    /**
     * 从原始字符串解析 Sentinel 版本
     *
     * @param verStr 版本字符串
     * @return 版本合法时返回解析出的 {@link SentinelVersion}；格式有误时返回 empty
     */
    public static Optional<SentinelVersion> parseVersion(String verStr) {
        if (StringUtil.isBlank(verStr)) {
            return Optional.empty();
        }
        try {
            String versionFull = verStr;
            SentinelVersion version = new SentinelVersion();

            // 解析后缀
            int index = versionFull.indexOf("-");
            if (index == 0) {
                // 以 "-" 开头，格式非法
                return Optional.empty();
            }
            if (index > 0 && index != versionFull.length() - 1) {
                // "-" 在中间：其后为后缀；末尾的 "-" 表示无后缀
                version.setPostfix(versionFull.substring(index + 1));
            }

            if (index >= 0) {
                versionFull = versionFull.substring(0, index);
            }

            // 解析 x.x.x 三段版本号
            int segment = 0;
            int[] ver = new int[3];
            while (segment < ver.length) {
                index = versionFull.indexOf('.');
                if (index < 0) {
                    if (versionFull.length() > 0) {
                        ver[segment] = Integer.valueOf(versionFull);
                    }
                    break;
                }
                ver[segment] = Integer.valueOf(versionFull.substring(0, index));
                versionFull = versionFull.substring(index + 1);
                segment++;
            }

            if (ver[0] < 1) {
                // 主版本号小于 1，视为格式非法
                return Optional.empty();
            } else {
                return Optional.of(version
                        .setMajorVersion(ver[0])
                        .setMinorVersion(ver[1])
                        .setFixVersion(ver[2]));
            }
        } catch (Exception ex) {
            // 解析异常时返回 empty
            return Optional.empty();
        }
    }

    /** 私有构造方法，防止实例化 */
    private VersionUtils() {}
}
