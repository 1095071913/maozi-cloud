package com.maozi.oauth.config;

import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.properties.ApiWhitelistProperties;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * 资源服务器安全配置
 * <p>
 * 配置 OAuth2 资源服务器的安全策略，包括 API 白名单放行、
 * 不透明令牌（Opaque Token）认证、自定义访问拒绝和认证入口处理器。
 * 启用方法级安全注解（JSR-250 和 Secured）支持。
 * </p>
 *
 * @author maozi
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
public class ResourceServerConfig {

    /** API 白名单配置 */
    @Resource
    public ApiWhitelistProperties apiWhitelistProperties;

    /**
     * 创建默认安全过滤器链
     * <p>
     * 配置白名单路径放行、其余请求需要认证，以及 OAuth2 资源服务器的
     * 不透明令牌认证和自定义异常处理器。
     * </p>
     *
     * @param http HTTP 安全构建器
     * @return 安全过滤器链
     * @throws Exception 安全配置异常
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);

        // 合并系统默认白名单和项目自定义白名单
        List<String> whitelist = CollectionUtil.newArrayList();
        whitelist.addAll(ApiWhitelistProperties.DEFAULT_WITE_LIST);

        List<String> configWhitelist = apiWhitelistProperties.getConfigWhitelist();
        if(ObjectUtil.isNotNullEmpty(configWhitelist)){
            whitelist.addAll(configWhitelist);
        }

        // 将白名单列表转换为数组，用于Spring Security路径匹配
        String[] requestMatchers = whitelist.toArray(new String[0]);

        // 配置请求授权规则：白名单路径放行，其余所有请求需要认证
        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers(requestMatchers).permitAll()
                .anyRequest().authenticated());

        // 添加BearerTokenAuthenticationFilter，将认证服务当做一个资源服务器，解析请求头中的token
        http.oauth2ResourceServer((resourceServer) -> resourceServer
                .opaqueToken(Customizer.withDefaults())  // 使用不透明令牌（Opaque Token）方式进行令牌校验
                .accessDeniedHandler(new AccessDeniedHandler())  // 自定义权限不足时的响应处理
                .authenticationEntryPoint(new AuthenticationEntryPoint())  // 自定义未认证时的响应处理
        );

        return http.build();

    }

    /**
     * 创建令牌认证管理器解析器
     * <p>
     * 配置不透明令牌认证提供者，所有请求统一使用不透明令牌认证方式。
     * </p>
     *
     * @param opaqueTokenIntrospector 不透明令牌内省器
     * @return 认证管理器解析器
     */
    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> tokenAuthenticationManagerResolver(OpaqueTokenIntrospector opaqueTokenIntrospector) {
        // 使用自定义的不透明令牌内省器创建认证提供者，并封装为ProviderManager
        AuthenticationManager opaqueToken = new ProviderManager(new OpaqueTokenAuthenticationProvider(opaqueTokenIntrospector));
        // 所有请求统一返回同一个AuthenticationManager（不透明令牌认证管理器）
        return (request) -> opaqueToken;
    }

}
