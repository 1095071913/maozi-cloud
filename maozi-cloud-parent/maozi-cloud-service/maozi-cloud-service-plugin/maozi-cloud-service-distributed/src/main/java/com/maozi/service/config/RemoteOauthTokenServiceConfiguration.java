package com.maozi.service.config;

import com.maozi.oauth.api.service.RemoteOauthTokenServiceImpl;
import com.maozi.oauth.token.api.OauthTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 资源服务器令牌内省服务自动装配。
 * <p>
 * 仅当容器中不存在任何 {@link OauthTokenService} 实现时（即非授权服务器进程），
 * 才注册 {@link RemoteOauthTokenServiceImpl} 作为远程调用代理实现
 * （默认走 Dubbo RPC，可按配置切换为 REST）。
 * 授权服务器进程已有本地 {@code OauthTokenServiceImpl}，本配置不会生效。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/6/26
 */
@Configuration
public class RemoteOauthTokenServiceConfiguration {

    /**
     * 注册远程令牌内省服务
     * <p>
     * 仅当容器中不存在 {@link OauthTokenService} 实现时（非授权服务器进程）生效。
     * </p>
     *
     * @return 远程调用代理实现的令牌服务（默认 Dubbo RPC，可按配置切换为 REST）
     */
    @Bean
    @ConditionalOnMissingBean(OauthTokenService.class)
    public OauthTokenService oauthTokenService() {
        return new RemoteOauthTokenServiceImpl();
    }

}
