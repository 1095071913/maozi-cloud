package com.maozi.oauth.config;

import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.oauth.token.api.OauthTokenService;
import com.maozi.oauth.token.constants.OAuth2TokenUserClaimConstants;
import jakarta.annotation.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 不透明令牌内省器
 * <p>
 * 内省逻辑委托给注入的 {@link OauthTokenService}：授权服务器进程为本地实现（进程内调用），
 * 资源服务器进程为 {@code RemoteOauthTokenServiceImpl}，按配置选择 Dubbo RPC（默认）或 HTTP 远程调用。
 * 内省完成后从返回的 claims 中提取 authorities 构建 {@link GrantedAuthority} 权限集合，用于访问控制决策。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class OpaqueTokenIntrospector implements org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector {

    /** 令牌内省服务（授权服务器进程为本地实现，资源服务器进程为远程实现） */
    @Resource(name = "oauthTokenService")
    private OauthTokenService oauthTokenService;

    /**
     * 内省令牌并提取权限信息
     *
     * @param token 待内省的令牌字符串
     * @return 包含权限信息的认证主体
     * @throws BadOpaqueTokenException 令牌无效，或内省服务抛出 {@code OAuth2IntrospectionException}/{@code BusinessResultException} 时统一转换为本异常
     */
    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        try {

            // 委托内省服务完成令牌校验
            Map<String, Object> claims = oauthTokenService.introspect(token);

            // 校验令牌活跃状态并构建带权限的认证主体
            return buildPrincipalFromClaims(claims);

        } catch (BadOpaqueTokenException e) {
            // 令牌无效异常直接抛出，由上层框架处理（返回401）
            throw e;
        } catch (OAuth2IntrospectionException e) {
            // OAuth2内省过程中的其他异常，转换为令牌无效异常并包装为系统错误
            throw new BadOpaqueTokenException(e.getMessage(), new BusinessResultException(SystemErrorCode.SYSTEM_ERROR).setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE));
        }catch (BusinessResultException e){
            // 业务异常（如RPC调用返回的业务错误），转换为令牌无效异常并保留原始业务错误信息
            throw new BadOpaqueTokenException(e.getMessage(), new BusinessResultException(e.getErrorResult()));
        }
    }

    /**
     * 从令牌内省返回的 claims 构建带权限的认证主体
     *
     * @param claims 令牌内省返回的声明信息，至少包含 active 字段
     * @return 包含权限信息的认证主体
     * @throws BadOpaqueTokenException 令牌不活跃时抛出
     */
    private OAuth2AuthenticatedPrincipal buildPrincipalFromClaims(Map<String, Object> claims) {
        // 检查令牌是否处于活跃状态
        Boolean active = (Boolean) claims.get(OAuth2TokenIntrospectionClaimNames.ACTIVE);
        if (active == null || !active) {
            throw new BadOpaqueTokenException("Token is not active");
        }

        Set<GrantedAuthority> authorities = extractAuthorities(claims);
        // 主体名称取 sub 声明，缺省为 unknown
        return new DefaultOAuth2AuthenticatedPrincipal(
                (String) claims.getOrDefault(OAuth2TokenIntrospectionClaimNames.SUB, "unknown"), claims, authorities);
    }

    /**
     * 从 claims 中提取权限
     *
     * @param claims 令牌内省返回的声明信息
     * @return 权限集合，没有 authorities 字段时返回空集合
     */
    private Set<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Object authoritiesObj = claims.get(OAuth2TokenUserClaimConstants.AUTHORITIES);
        // 如果authorities不存在或不是List类型，返回空权限集合
        if (!(authoritiesObj instanceof List<?> list)) {
            return Collections.emptySet();
        }
        return list.stream()
                .map(item -> new SimpleGrantedAuthority(item.toString()))
                .collect(Collectors.toSet());
    }

}
