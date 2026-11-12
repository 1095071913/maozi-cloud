package com.maozi.oauth.config;

import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.oauth.token.api.OauthTokenService;
import com.maozi.oauth.token.constants.OAuth2TokenClaimConstants;
import jakarta.annotation.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
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
 * 内省逻辑委托给注入的 {@link OauthTokenService} 完成：授权服务器进程注入的是本地实现
 * （进程内直接调用，无网络开销）；资源服务器进程注入的是 {@code RemoteOauthTokenServiceImpl}，
 * 由其根据配置 {@code spring.security.oauth2.resourceserver.opaquetoken.mode} 选择远程调用方式：
 * <ul>
 *   <li>{@code rpc}（默认，配置为空时生效）：通过 Dubbo RPC 直接调用 OAuth 授权服务器的内省服务，减少网络开销</li>
 *   <li>其他值：通过 HTTP（Feign）调用授权服务器的内省接口</li>
 * </ul>
 * 内省完成后，从响应中提取 authorities 字段并转换为 {@link GrantedAuthority} 集合，
 * 使权限信息可用于后续的访问控制决策。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class OpaqueTokenIntrospector implements org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector {

    /** 令牌内省服务：授权服务器进程为本地实现，资源服务器进程为 RemoteOauthTokenServiceImpl（按配置选择 Dubbo RPC 或 HTTP 远程调用） */
    @Resource(name = "oauthTokenService")
    private OauthTokenService oauthTokenService;

    /**
     * 内省令牌并提取权限信息
     * <p>
     * 委托 {@link #oauthTokenService} 完成内省：本进程为授权服务器时为进程内本地调用，
     * 资源服务器进程时由该服务按配置模式选择 Dubbo RPC 或 HTTP（Feign）远程调用；
     * 内省完成后从响应中提取 authorities 并构建带权限的认证主体。
     * </p>
     *
     * @param token 待内省的令牌字符串
     * @return 包含权限信息的认证主体
     * @throws BadOpaqueTokenException 令牌无效；内省服务抛出的 {@code OAuth2IntrospectionException}
     *                                  与 {@code BusinessResultException} 也会被捕获并转换为本异常抛出，
     *                                  本方法对外只会抛出 {@code BadOpaqueTokenException}
     */
    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        try {

            // 委托令牌内省服务：授权服务器进程为进程内本地调用；资源服务器进程由
            // RemoteOauthTokenServiceImpl 按配置模式选择 Dubbo RPC（默认）或 HTTP 调用
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
     * 从令牌内省返回的claims构建带权限的认证主体
     * <p>
     * 本地内省与 Dubbo RPC 内省返回的均为 claims Map，统一通过本方法
     * 校验令牌活跃状态并构建包含权限信息的认证主体。
     * </p>
     *
     * @param claims 令牌内省返回的声明信息，至少包含 active 字段
     * @return 包含权限信息的认证主体
     * @throws BadOpaqueTokenException 令牌不活跃时抛出
     */
    private OAuth2AuthenticatedPrincipal buildPrincipalFromClaims(Map<String, Object> claims) {
        // 检查令牌是否处于活跃状态
        Boolean active = (Boolean) claims.get(OAuth2TokenClaimConstants.ACTIVE);
        if (active == null || !active) {
            throw new BadOpaqueTokenException("Token is not active");
        }

        // 从claims中提取权限信息
        Set<GrantedAuthority> authorities = extractAuthorities(claims);
        // 构建认证主体，包含主体名称（sub字段）、全部声明信息和权限集合
        return new DefaultOAuth2AuthenticatedPrincipal(
                (String) claims.getOrDefault(OAuth2TokenClaimConstants.SUB, "unknown"), claims, authorities);
    }

    /**
     * 从claims中提取权限
     * <p>
     * 将claims中的authorities字段（字符串列表）转换为Spring Security的
     * {@link GrantedAuthority} 集合。
     * </p>
     *
     * @param claims 令牌内省返回的声明信息
     * @return 权限集合，如果没有authorities字段则返回空集合
     */
    private Set<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Object authoritiesObj = claims.get(OAuth2TokenClaimConstants.AUTHORITIES);
        // 如果authorities不存在或不是List类型，返回空权限集合
        if (!(authoritiesObj instanceof List<?> list)) {
            return Collections.emptySet();
        }
        // 将每个权限字符串转换为SimpleGrantedAuthority
        return list.stream()
                .map(item -> new SimpleGrantedAuthority(item.toString()))
                .collect(Collectors.toSet());
    }

}
