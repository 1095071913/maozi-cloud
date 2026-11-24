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

import com.alibaba.csp.sentinel.dashboard.auth.AuthService;
import com.alibaba.csp.sentinel.dashboard.auth.SimpleWebAuthServiceImpl;
import com.alibaba.csp.sentinel.dashboard.config.DashboardConfig;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 控制台的登录、登出与登录状态校验控制器。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.6.0
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** 日志记录器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    /** 登录用户名，来自 auth.username 属性，默认 "sentinel"。 */
    @Value("${auth.username:sentinel}")
    private String authUsername;

    /** 登录密码，来自 auth.password 属性，默认 "sentinel"。 */
    @Value("${auth.password:sentinel}")
    private String authPassword;

    /** 用于校验登录状态的认证服务。 */
    private final AuthService<HttpServletRequest> authService;

    /** 注入认证服务。 */
    public AuthController(AuthService<HttpServletRequest> authService) {
        this.authService = authService;
    }

    /**
     * 用户登录：sentinel.dashboard.auth.*（环境变量或系统属性）优先于
     * auth.username/auth.password 属性，登录成功后将认证用户存入 Web session。
     */
    @PostMapping("/login")
    public Result<AuthService.AuthUser> login(HttpServletRequest request, String username, String password) {
        // 缓存认证配置，避免同一方法内重复获取
        String configAuthUsername = DashboardConfig.getAuthUsername();
        String configAuthPassword = DashboardConfig.getAuthPassword();
        if (StringUtils.isNotBlank(configAuthUsername)) {
            authUsername = configAuthUsername;
        }

        if (StringUtils.isNotBlank(configAuthPassword)) {
            authPassword = configAuthPassword;
        }

        // 若 auth.username 或 auth.password 为空（在 application.properties 或 VM 参数中设置），
        // 则校验直接通过：前端已保证输入非空，此时用户可用任意非空的用户名和密码登录。
        boolean usernameMismatch = StringUtils.isNotBlank(authUsername) && !authUsername.equals(username);
        boolean passwordMismatch = StringUtils.isNotBlank(authPassword) && !authPassword.equals(password);
        if (usernameMismatch || passwordMismatch) {
            LOGGER.error("Login failed: Invalid username or password, username={}", username);
            return Result.ofFail(Result.CODE_ERROR, "Invalid username or password");
        }

        AuthService.AuthUser authUser = new SimpleWebAuthServiceImpl.SimpleWebAuthUserImpl(username);
        request.getSession().setAttribute(SimpleWebAuthServiceImpl.WEB_SESSION_KEY, authUser);
        return Result.ofSuccess(authUser);
    }

    /** 使当前 session 失效以完成登出。 */
    @PostMapping(value = "/logout")
    public Result<?> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return Result.ofSuccess(null);
    }

    /** 返回当前 session 的已登录用户，未登录时返回失败结果。 */
    @PostMapping(value = "/check")
    public Result<?> check(HttpServletRequest request) {
        AuthService.AuthUser authUser = authService.getAuthUser(request);
        if (authUser == null) {
            return Result.ofFail(Result.CODE_ERROR, "Not logged in");
        }
        return Result.ofSuccess(authUser);
    }
}
