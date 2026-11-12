package com.maozi.oauth.config;

import com.maozi.common.ObjectUtil;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.oauth.token.constants.OAuth2TokenClaimConstants;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 应用用户上下文拦截器
 * <p>
 * 在 HTTP 请求处理前从 Spring Security 上下文中提取当前认证用户 ID、clientId、用户名与权限列表，
 * 封装为 {@link CurrentUserInfo} 后存入 {@link ApplicationLinkContext} 线程本地变量。
 * 版本号和 traceId 的提取由 {@code ApplicationContextFilter}（config-web 模块）负责；
 * 请求结束后的上下文清理同样由 {@code ApplicationContextFilter} 统一完成，
 * 防止线程池复用导致的数据泄漏。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class ApplicationUserContextFilter implements HandlerInterceptor {

    /**
     * 请求处理前设置当前登录用户上下文
     * <p>
     * 从 Security 上下文中获取当前认证用户（必须是 {@link DefaultOAuth2AuthenticatedPrincipal}），
     * 提取用户 ID、clientId、用户名与权限列表存入线程本地变量。
     * </p>
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @return 始终返回 true，继续执行后续拦截器
     */
    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(ObjectUtil.isNotNullEmpty(authentication) && authentication.isAuthenticated() && authentication.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal authenticatedPrincipal){

            Long userId = authenticatedPrincipal.getAttribute(OAuth2TokenClaimConstants.USER_ID);

            // client_id 在令牌声明中以字符串形式存储，需转换为 Long
            Long clientId = null;
            String clientIdStr = authenticatedPrincipal.getAttribute(OAuth2TokenClaimConstants.CLIENT_ID);
            if(ObjectUtil.isNotNullEmpty(clientIdStr)){
                clientId = Long.parseLong(clientIdStr);
            }

            List<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            CurrentUserInfo currentUserInfo = new CurrentUserInfo(userId,clientId,authentication.getName(),authorities);
            ApplicationLinkContext.setCurrentUserInfo(currentUserInfo);
        }

        return true;

    }

}