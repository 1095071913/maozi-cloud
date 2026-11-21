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

import java.io.Serializable;

/**
 * 认证与授权服务接口。
 *
 * @author Carpenter Lee
 * @date 2026/11/22
 * @since 1.5.0
 */
public interface AuthService<R> {

    /**
     * 获取指定请求对应的已认证用户。
     *
     * @param request 携带用户信息的请求
     * @return 当前用户对应的认证用户对象，用户非法时返回 null
     */
    AuthUser getAuthUser(R request);

    /**
     * 权限类型。
     */
    enum PrivilegeType {
        /**
         * 查看规则
         */
        READ_RULE,
        /**
         * 新增或修改规则
         */
        WRITE_RULE,
        /**
         * 删除规则
         */
        DELETE_RULE,
        /**
         * 查看监控数据
         */
        READ_METRIC,
        /**
         * 新增机器
         */
        ADD_MACHINE,
        /**
         * 拥有以上全部权限
         */
        ALL
    }

    /**
     * 当前用户抽象。实现 Serializable 以便存入可能被容器持久化的 HTTP session。
     */
    interface AuthUser extends Serializable {

        /**
         * 判断当前用户是否对目标（应用名、IP 地址等）拥有指定权限。
         *
         * @param target        授权目标
         * @param privilegeType 权限类型
         * @return 拥有权限返回 true，否则返回 false
         */
        boolean authTarget(String target, PrivilegeType privilegeType);

        /**
         * 判断当前用户是否为超级用户。
         *
         * @return 是超级用户返回 true，否则返回 false
         */
        boolean isSuperUser();

        /**
         * 获取当前用户昵称。
         *
         * @return 当前用户昵称
         */
        String getNickName();

        /**
         * 获取当前用户登录名。
         *
         * @return 当前用户登录名
         */
        String getLoginName();

        /**
         * 获取当前用户 ID。
         *
         * @return 当前用户 ID
         */
        String getId();
    }
}
