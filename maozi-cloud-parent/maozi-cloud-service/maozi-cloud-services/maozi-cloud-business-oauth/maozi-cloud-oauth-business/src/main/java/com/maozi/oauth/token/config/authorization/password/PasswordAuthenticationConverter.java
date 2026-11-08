package com.maozi.oauth.token.config.authorization.password;

import com.maozi.oauth.token.config.authorization.base.BaseGrantAuthenticationConverter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * 密码模式认证转换器
 * <p>
 * 将HTTP请求中的密码模式（password grant type）参数转换为PasswordAuthenticationToken。
 * 负责校验请求中的用户名和密码参数是否为空。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordAuthenticationConverter extends BaseGrantAuthenticationConverter<PasswordAuthenticationToken> {

    /** 授权类型标识：密码模式 */
    private final String grantType = "password";

    /**
     * 校验请求参数
     * <p>
     * 检查请求中的username和password参数是否存在且有效。
     * 如果参数为空或存在多个值，则抛出OAuth2认证异常。
     * </p>
     *
     * @param request HTTP请求对象
     * @throws OAuth2AuthenticationException 当用户名或密码为空或存在多个值时抛出
     */
    @Override
    public void checkParams(HttpServletRequest request) {

        MultiValueMap<String, String> parameters = getParameters(request);

        String username = parameters.getFirst("username");
        if (!StringUtils.hasText(username) || parameters.get("username").size() != 1) {
            throw new OAuth2AuthenticationException(new OAuth2Error("400","账号不能为空",ACCESS_TOKEN_REQUEST_ERROR_URI));
        }

        String password = parameters.getFirst("password");
        if (!StringUtils.hasText(password) || parameters.get("password").size() != 1) {
            throw new OAuth2AuthenticationException(new OAuth2Error("400", "密码不能为空", ACCESS_TOKEN_REQUEST_ERROR_URI));
        }

    }

}
