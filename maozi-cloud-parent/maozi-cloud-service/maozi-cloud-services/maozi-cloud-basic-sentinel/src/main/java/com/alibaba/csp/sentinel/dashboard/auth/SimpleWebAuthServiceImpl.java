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
package com.alibaba.csp.sentinel.dashboard.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 面向 Web 请求的简单 {@link AuthService} 实现，从 HTTP session 属性
 * {@link #WEB_SESSION_KEY} 中解析已登录用户。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.6.0
 */
public class SimpleWebAuthServiceImpl implements AuthService<HttpServletRequest> {

    /** 存储已登录 Sentinel 管理员的 session 属性键。 */
    public static final String WEB_SESSION_KEY = "session_sentinel_admin";

    /** {@inheritDoc} 返回 session 中存储的 {@link AuthUser}，不存在或类型不符时返回 null。 */
    @Override
    public AuthUser getAuthUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object sentinelUserObj = session.getAttribute(WEB_SESSION_KEY);
        if (sentinelUserObj instanceof AuthUser) {
            return (AuthUser) sentinelUserObj;
        }
        return null;
    }

    /**
     * 仅以用户名标识的认证用户，视为超级用户，所有权限校验均通过。
     *
     * @author cdfive
     * @date 2026/11/22
     */
    public static final class SimpleWebAuthUserImpl implements AuthUser {

        /** 序列化版本号 */
        private static final long serialVersionUID = 1L;

        /** 登录用户名。 */
        private final String username;

        /** 使用指定用户名构建认证用户。 */
        public SimpleWebAuthUserImpl(String username) {
            this.username = username;
        }

        /** {@inheritDoc} 始终返回 true。 */
        @Override
        public boolean authTarget(String target, PrivilegeTypeEnum privilegeType) {
            return true;
        }

        /** {@inheritDoc} 始终返回 true。 */
        @Override
        public boolean isSuperUser() {
            return true;
        }

        /** {@inheritDoc} 返回用户名作为昵称。 */
        @Override
        public String getNickName() {
            return username;
        }

        /** {@inheritDoc} 返回用户名作为登录名。 */
        @Override
        public String getLoginName() {
            return username;
        }

        /** {@inheritDoc} 返回用户名作为用户 ID。 */
        @Override
        public String getId() {
            return username;
        }
    }
}
