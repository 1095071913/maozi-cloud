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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记控制器方法需要指定权限才可访问的注解。
 *
 * @author lkxiaolou
 * @date 2026/11/22
 * @since 1.7.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.METHOD})
public @interface AuthAction {

    /**
     * @return 所需的权限类型
     */
    AuthService.PrivilegeTypeEnum value();

    /**
     * @return 携带授权目标（应用名）的请求参数名，默认 "app"
     */
    String targetName() default "app";

    /**
     * @return 权限不足时的提示消息
     */
    String message() default "Permission denied";
}
