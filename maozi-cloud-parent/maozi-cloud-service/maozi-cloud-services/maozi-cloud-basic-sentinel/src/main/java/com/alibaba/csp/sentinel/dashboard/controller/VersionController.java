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
package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供 Sentinel 控制台版本号查询的控制器。
 *
 * @author hisenyuan
 * @date 2026/11/22
 * @since 1.7.0
 */
@RestController
public class VersionController {

    /** 版本串截断分隔符：仅返回其之前的部分。 */
    private static final String VERSION_PATTERN = "-";

    /** 从 sentinel.dashboard.version 属性注入的控制台版本号。 */
    @Value("${sentinel.dashboard.version:}")
    private String sentinelDashboardVersion;

    /** 返回截取首个 "-" 之前的控制台版本号，未配置时返回错误结果。 */
    @GetMapping("/version")
    public Result<String> apiGetVersion() {
        if (StringUtil.isBlank(sentinelDashboardVersion)) {
            return Result.ofFail(Result.CODE_FAIL, "getVersion failed: empty version");
        }
        int separatorIndex = sentinelDashboardVersion.indexOf(VERSION_PATTERN);
        if (separatorIndex < 0) {
            return Result.ofSuccess(sentinelDashboardVersion);
        }
        return Result.ofSuccess(sentinelDashboardVersion.substring(0, separatorIndex));
    }
}
