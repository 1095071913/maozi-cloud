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

import javax.servlet.Filter;

/**
 * <p>用于登录认证的 Servlet 过滤器接口。</p>
 *
 * <p>注意：部分 URL 无需认证，直接放行，包括：</p>
 * <ul>
 * <li>首页地址：{@code /}</li>
 * <li>认证请求地址：{@code /login}、{@code /logout}</li>
 * <li>机器注册地址：{@code /registry/machine}</li>
 * <li>静态资源</li>
 * </ul>
 * <p>
 * 排除的 URL 与后缀均可在 {@code application.properties} 中配置。
 *
 * @author cdfive
 * @author wxq
 * @date 2026/11/22
 * @since 1.6.0
 */
public interface LoginAuthenticationFilter extends Filter {

}
