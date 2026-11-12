package com.maozi.oauth.token.config.authorization.base;

import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * 子认证提供者基类
 * <p>
 * 抽象基类，为子认证流程提供统一模板。通过泛型机制自动识别支持的认证Token类型，
 * 并将认证逻辑委托给子类实现的subAuthenticate方法。
 * </p>
 *
 * @param <T> 认证令牌类型，必须继承自AbstractAuthenticationToken
 * @author maozi
 * @date 2026/11/08
 */
public abstract class BaseSubAuthenticationProvider<T extends AbstractAuthenticationToken> implements AuthenticationProvider {

    /** 当前泛型对应的Token令牌Class对象，用于判断支持的认证类型 */
    protected Class<T> tokenClass = currentTokenClass();

    /**
     * 通过反射获取当前子类的泛型Token类型
     *
     * @return 泛型Token的Class对象
     */
    protected Class<T> currentTokenClass() {
        return (Class<T>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseSubAuthenticationProvider.class, 0);
    }

    /**
     * 判断是否支持该认证类型
     *
     * @param authentication 认证类型的Class对象
     * @return 如果是当前泛型Token类型则返回true
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return tokenClass.isAssignableFrom(authentication);
    }

    /**
     * 执行认证，将认证请求委托给子类的subAuthenticate方法
     *
     * @param authentication 认证请求对象
     * @return 认证结果
     * @throws AuthenticationException 认证过程中出现的异常
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return subAuthenticate((T) authentication);
    }

    /**
     * 子类实现的子认证逻辑
     *
     * @param authentication 认证Token对象
     * @return 认证结果
     */
    public abstract Authentication subAuthenticate(T authentication);

}
