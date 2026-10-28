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
 * A simple {@link AuthService} implementation for web requests, which resolves the
 * logged-in user from the HTTP session attribute {@link #WEB_SESSION_KEY}.
 *
 * @author cdfive
 * @since 1.6.0
 */
public class SimpleWebAuthServiceImpl implements AuthService<HttpServletRequest> {

    /** Session attribute key that stores the logged-in Sentinel admin user. */
    public static final String WEB_SESSION_KEY = "session_sentinel_admin";

    /** Returns the {@link AuthUser} stored in the session, or null if absent or not an AuthUser. */
    @Override
    public AuthUser getAuthUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object sentinelUserObj = session.getAttribute(SimpleWebAuthServiceImpl.WEB_SESSION_KEY);
        if (sentinelUserObj != null && sentinelUserObj instanceof AuthUser) {
            return (AuthUser) sentinelUserObj;
        }

        return null;
    }

    /** Auth user identified only by username and treated as a super user that passes all checks. */
    public static final class SimpleWebAuthUserImpl implements AuthUser {

        /** The username of the logged-in user. */
        private String username;

        /** Creates the auth user with the given username. */
        public SimpleWebAuthUserImpl(String username) {
            this.username = username;
        }

        /** {@inheritDoc} Always returns true. */
        @Override
        public boolean authTarget(String target, PrivilegeType privilegeType) {
            return true;
        }

        /** {@inheritDoc} Always returns true. */
        @Override
        public boolean isSuperUser() {
            return true;
        }

        /** Get the username as nick name. */
        @Override
        public String getNickName() {
            return username;
        }

        /** Get the username as login name. */
        @Override
        public String getLoginName() {
            return username;
        }

        /** Get the username as user id. */
        @Override
        public String getId() {
            return username;
        }
    }
}
