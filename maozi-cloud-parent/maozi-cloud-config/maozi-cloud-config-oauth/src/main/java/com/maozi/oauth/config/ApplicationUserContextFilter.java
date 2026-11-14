package com.maozi.oauth.config;

import com.maozi.common.ObjectUtil;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.oauth.token.constants.OAuth2TokenUserClaimConstants;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 应用用户上下文拦截器
 * <p>
 * 在 HTTP 请求处理前从 Spring Security 上下文中提取当前认证用户 ID、clientId、用户名与权限列表，
 * 封装为 {@link CurrentUserInfo} 后存入 {@link CurrentUserInfoContext} 线程本地变量；
 * 上下文的清理由 config-web 模块的 {@code ApplicationContextFilter} 统一完成，防止线程复用导致数据泄漏。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class ApplicationUserContextFilter implements HandlerInterceptor {

    /**
     * 请求处理前从 Security 上下文提取认证用户信息并存入线程上下文，始终返回 true
     */
    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {

        // 获取当前认证信息（OAuth2 认证时主体为 DefaultOAuth2AuthenticatedPrincipal）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(ObjectUtil.isNotNullEmpty(authentication) && authentication.isAuthenticated() && authentication.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal authenticatedPrincipal){

            Long userId = authenticatedPrincipal.getAttribute(OAuth2TokenUserClaimConstants.USER_ID);

            // client_id 在令牌声明中以字符串形式存储，需转换为 Long
            Long clientId = null;
            String clientIdStr = authenticatedPrincipal.getAttribute(OAuth2TokenIntrospectionClaimNames.CLIENT_ID);
            if(ObjectUtil.isNotNullEmpty(clientIdStr)){
                clientId = Long.parseLong(clientIdStr);
            }

            // 封装用户信息（含权限列表）存入线程上下文
            List<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            CurrentUserInfo currentUserInfo = new CurrentUserInfo(userId, clientId, authentication.getName(), authorities);
            CurrentUserInfoContext.CONTEXT.set(currentUserInfo);
        }

        return true;

    }

}