package com.maozi.oauth.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 拦截器注册配置
 * <p>
 * 向 Spring MVC 注册 {@link ApplicationUserContextFilter}，
 * 用于在请求进入时将当前用户信息写入 {@code ApplicationLinkContext}
 * （版本号、traceId 由 config-web 模块的 ApplicationContextFilter 负责），
 * 拦截所有路径并将优先级设为最高（{@code Integer.MIN_VALUE}）。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/16 17:59
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    /** 应用用户上下文拦截器 */
    @Resource
    private ApplicationUserContextFilter applicationUserContextFilter;

    /**
     * 注册拦截器
     * <p>
     * 添加应用链路上下文过滤器，拦截所有路径，优先级设为最高。
     * </p>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(applicationUserContextFilter).addPathPatterns("/**").order(Integer.MIN_VALUE);
    }

}
