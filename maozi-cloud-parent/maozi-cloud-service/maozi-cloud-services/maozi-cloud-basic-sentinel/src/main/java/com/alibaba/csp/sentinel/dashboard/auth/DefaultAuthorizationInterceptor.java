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

import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.fastjson.JSON;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 基于权限校验的 Web 授权拦截器默认实现。
 * <p>
 * 由旧的 {@link AuthorizationInterceptor} 迁移而来。
 *
 * @author lkxiaolou
 * @author wxq
 * @date 2026/11/22
 * @since 1.7.1
 */
public class DefaultAuthorizationInterceptor implements AuthorizationInterceptor {

    /** HTTP Content-Type 请求头名称。 */
    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    /** 带 UTF-8 编码的 JSON 内容类型，用于权限校验失败时的响应体。 */
    private static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";

    /** 用于从请求中解析当前用户的认证服务。 */
    private final AuthService<HttpServletRequest> authService;

    /** 使用指定的认证服务构建拦截器。 */
    public DefaultAuthorizationInterceptor(AuthService<HttpServletRequest> authService) {
        this.authService = authService;
    }

    /**
     * 对标注了 {@link AuthAction} 的处理器方法做权限校验：从请求解析当前用户，
     * 并校验注解指定名称的目标参数；未标注的方法直接放行，未认证或无权限的请求
     * 以 JSON 错误消息拒绝。
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  处理器对象
     * @return 校验通过返回 true，否则返回 false
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            Method method = ((HandlerMethod) handler).getMethod();

            AuthAction authAction = method.getAnnotation(AuthAction.class);
            if (authAction != null) {
                AuthService.AuthUser authUser = authService.getAuthUser(request);
                if (authUser == null) {
                    responseNoPrivilegeMsg(response, authAction.message());
                    return false;
                }
                String target = request.getParameter(authAction.targetName());

                if (!authUser.authTarget(target, authAction.value())) {
                    responseNoPrivilegeMsg(response, authAction.message());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 将权限校验失败消息以 JSON 形式写入响应。
     *
     * @param response 当前响应
     * @param message  失败提示消息
     * @throws IOException 写入响应流出错时抛出
     */
    private void responseNoPrivilegeMsg(HttpServletResponse response, String message) throws IOException {
        Result result = Result.ofFail(Result.CODE_ERROR, message);
        response.addHeader(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8);
        response.getOutputStream().write(JSON.toJSONBytes(result));
    }

}
