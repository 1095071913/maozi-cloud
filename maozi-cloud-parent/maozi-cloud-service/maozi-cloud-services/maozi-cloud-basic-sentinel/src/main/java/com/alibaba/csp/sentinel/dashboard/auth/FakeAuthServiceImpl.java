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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * 伪认证服务实现，所有用户权限校验均直接通过。
 *
 * @author Carpenter Lee
 * @date 2026/11/22
 * @since 1.5.0
 */
public class FakeAuthServiceImpl implements AuthService<HttpServletRequest> {

    /** 用于警告未配置真实认证实现的日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(FakeAuthServiceImpl.class);

    /** 构造时记录警告日志，提示当前使用伪实现跳过了全部权限校验。 */
    public FakeAuthServiceImpl() {
        logger.warn("there is no auth, use {} by implementation {}", AuthService.class, FakeAuthServiceImpl.class);
    }

    /** {@inheritDoc} 始终返回一个通过全部权限校验的伪用户。 */
    @Override
    public AuthUser getAuthUser(HttpServletRequest request) {
        return new AuthUserImpl();
    }

    /**
     * 伪认证用户，所有权限校验均返回 true，身份信息为固定伪值。
     *
     * @author Carpenter Lee
     * @date 2026/11/22
     */
    static final class AuthUserImpl implements AuthUser {

        /** 序列化版本号 */
        private static final long serialVersionUID = 1L;

        /** {@inheritDoc} 始终返回 true，不校验目标与权限。 */
        @Override
        public boolean authTarget(String target, PrivilegeTypeEnum privilegeType) {
            return true;
        }

        /** {@inheritDoc} 始终返回 true。 */
        @Override
        public boolean isSuperUser() {
            return true;
        }

        /** {@inheritDoc} 返回固定的伪昵称。 */
        @Override
        public String getNickName() {
            return "FAKE_NICK_NAME";
        }

        /** {@inheritDoc} 返回固定的伪登录名。 */
        @Override
        public String getLoginName() {
            return "FAKE_LOGIN_NAME";
        }

        /** {@inheritDoc} 返回固定的伪用户 ID。 */
        @Override
        public String getId() {
            return "FAKE_EMP_ID";
        }
    }
}
