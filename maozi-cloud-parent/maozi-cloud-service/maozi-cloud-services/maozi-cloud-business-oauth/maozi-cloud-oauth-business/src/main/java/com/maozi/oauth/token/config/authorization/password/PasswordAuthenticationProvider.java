package com.maozi.oauth.token.config.authorization.password;

import com.maozi.oauth.token.config.authorization.base.BaseGrantAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 密码模式认证提供者
 * <p>
 * 处理密码模式（password grant type）的用户认证逻辑。
 * 从认证令牌的附加参数中提取用户名和密码，
 * 构建未认证的UsernamePasswordAuthenticationToken交由父类进行密码校验。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
@RequiredArgsConstructor
public class PasswordAuthenticationProvider extends BaseGrantAuthenticationProvider<PasswordAuthenticationToken> {

    /**
     * 执行密码模式的用户子认证
     * <p>
     * 从PasswordAuthenticationToken的附加参数中获取用户名和密码，
     * 创建一个未认证的UsernamePasswordAuthenticationToken返回给父类进行认证。
     * </p>
     *
     * @param authentication 密码模式认证令牌
     * @return 包含用户名和密码的未认证令牌
     */
    @Override
    public UsernamePasswordAuthenticationToken subAuthenticate(PasswordAuthenticationToken authentication) {

        // 从附加参数中取出用户名和密码
        Map<String, Object> additionalParameters = authentication.getAdditionalParameters();
        String username = (String) additionalParameters.get("username");
        String password = (String) additionalParameters.get("password");

        return UsernamePasswordAuthenticationToken.unauthenticated(username,password);

    }

}
