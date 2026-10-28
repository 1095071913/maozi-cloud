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
 * A fake AuthService implementation, which will pass all user auth checking.
 *
 * @author Carpenter Lee
 * @since 1.5.0
 */
public class FakeAuthServiceImpl implements AuthService<HttpServletRequest> {

    /** Logger that warns about the missing real auth implementation. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Logs a warning that this fake implementation bypasses all auth checking. */
    public FakeAuthServiceImpl() {
        this.logger.warn("there is no auth, use {} by implementation {}", AuthService.class, this.getClass());
    }

    /** {@inheritDoc} Always returns a fake user that passes all auth checking. */
    @Override
    public AuthUser getAuthUser(HttpServletRequest request) {
        return new AuthUserImpl();
    }

    /** Fake auth user that passes all privilege checking with fixed fake identity values. */
    static final class AuthUserImpl implements AuthUser {

        /** {@inheritDoc} Always returns true, ignoring the target and privilege. */
        @Override
        public boolean authTarget(String target, PrivilegeType privilegeType) {
            // fake implementation, always return true
            return true;
        }

        /** {@inheritDoc} Always returns true. */
        @Override
        public boolean isSuperUser() {
            // fake implementation, always return true
            return true;
        }

        /** Get the fixed fake nick name. */
        @Override
        public String getNickName() {
            return "FAKE_NICK_NAME";
        }

        /** Get the fixed fake login name. */
        @Override
        public String getLoginName() {
            return "FAKE_LOGIN_NAME";
        }

        /** Get the fixed fake user id. */
        @Override
        public String getId() {
            return "FAKE_EMP_ID";
        }
    }
}
