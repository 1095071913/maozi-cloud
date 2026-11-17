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

/**
 * Interface for authentication and authorization.
 *
 * @author Carpenter Lee
 * @since 1.5.0
 */
public interface AuthService<R> {

    /**
     * Get the authenticated user of the given request.
     *
     * @param request the request that carries the user information
     * @return the auth user representing the current user, or null if the user is illegal
     */
    AuthUser getAuthUser(R request);

    /**
     * Privilege type.
     */
    enum PrivilegeType {
        /**
         * Read rule
         */
        READ_RULE,
        /**
         * Create or modify rule
         */
        WRITE_RULE,
        /**
         * Delete rule
         */
        DELETE_RULE,
        /**
         * Read metrics
         */
        READ_METRIC,
        /**
         * Add machine
         */
        ADD_MACHINE,
        /**
         * All privileges above are granted.
         */
        ALL
    }

    /**
     * Represents the current user.
     */
    interface AuthUser {

        /**
         * Check whether the current user has the given privilege on the target, which may
         * be an app name, an ip address or other destination.
         *
         * @param target        the target to check
         * @param privilegeType the privilege type to check
         * @return true if the user has the privilege on the target, false otherwise
         */
        boolean authTarget(String target, PrivilegeType privilegeType);

        /**
         * Check whether current user is a super-user.
         *
         * @return true if the current user is a super user, false otherwise
         */
        boolean isSuperUser();

        /**
         * Get current user's nick name.
         *
         * @return current user's nick name.
         */
        String getNickName();

        /**
         * Get current user's login name.
         *
         * @return current user's login name.
         */
        String getLoginName();

        /**
         * Get current user's ID.
         *
         * @return ID of current user
         */
        String getId();
    }
}
