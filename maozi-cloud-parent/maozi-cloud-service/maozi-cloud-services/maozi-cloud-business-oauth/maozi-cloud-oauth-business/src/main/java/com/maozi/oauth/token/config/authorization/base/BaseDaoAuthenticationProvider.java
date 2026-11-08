package com.maozi.oauth.token.config.authorization.base;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 基础DAO认证提供者
 * <p>
 * 继承自Spring Security的DaoAuthenticationProvider，用于基于数据库的用户认证。
 * 重写了附加认证检查逻辑，当凭证为空时跳过密码校验，
 * 当密码错误时抛出自定义的中文错误提示"用户认证授权失败"。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
public class BaseDaoAuthenticationProvider extends DaoAuthenticationProvider {

    /**
     * 构造函数，初始化用户详情服务和密码编码器
     *
     * @param userDetailsService 用户详情服务，用于加载用户信息
     * @param passwordEncoder    密码编码器，用于密码的加密和匹配
     */
    public BaseDaoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        super.setPasswordEncoder(passwordEncoder);
        super.setUserDetailsService(userDetailsService);
    }

    /**
     * 附加认证检查
     * <p>
     * 对用户凭证进行额外校验。如果认证对象中没有凭证（如某些特殊认证方式），则直接跳过校验。
     * 如果密码不匹配，则捕获BadCredentialsException并抛出自定义错误信息"用户认证授权失败"。
     * </p>
     *
     * @param userDetails    加载的用户详情信息
     * @param authentication 包含用户凭证的认证令牌
     * @throws AuthenticationException 认证失败时抛出的异常
     */
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {

        if (authentication.getCredentials() == null) {
            return;
        }

        try{super.additionalAuthenticationChecks(userDetails, authentication);}catch (BadCredentialsException e){
            throw new BadCredentialsException("用户认证授权失败");
        }

    }

}
