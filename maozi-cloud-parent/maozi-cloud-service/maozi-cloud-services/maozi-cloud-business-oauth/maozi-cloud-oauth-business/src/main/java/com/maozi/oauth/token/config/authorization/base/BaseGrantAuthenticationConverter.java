package com.maozi.oauth.token.config.authorization.base;

import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 自定义授权模式Token转换器基类
 * <p>
 * 抽象基类，为各种自定义授权模式（如密码模式、短信验证码模式等）提供
 * HTTP请求到认证令牌的转换逻辑。子类只需实现授权类型获取、参数校验和
 * Token结果设置等特定逻辑即可。
 * </p>
 *
 * @param <T> 自定义授权令牌类型，必须继承自BaseGrantAuthenticationToken
 * @author vains
 * @date 2026/06/19
 */
public abstract class BaseGrantAuthenticationConverter<T extends BaseGrantAuthenticationToken> implements AuthenticationConverter {

    /** 令牌请求错误的参考文档URI，用于OAuth2错误响应中 */
    protected static final String ACCESS_TOKEN_REQUEST_ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

    /** 当前泛型对应的Token令牌Class对象，用于反射创建Token实例 */
    protected Class<T> tokenClass = currentTokenClass();

    /**
     * 通过反射获取当前子类的泛型Token类型
     *
     * @return 泛型Token的Class对象
     */
    protected Class<T> currentTokenClass() {
        return (Class<T>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseGrantAuthenticationConverter.class, 0);
    }

    /**
     * 授权类型
     *
     * @return 当前转换器支持的授权类型标识（对应grant_type参数值，如password）
     */
    protected abstract String getGrantType();

    /**
     * 校验参数
     * <p>
     * 子类可重写此方法校验各自授权模式所需的请求参数，默认为空实现。
     * </p>
     *
     * @param request HTTP请求对象
     */
    public void checkParams(HttpServletRequest request) {}

    /**
     * 设置Token结果
     * <p>
     * 在Token创建后，子类可重写此方法对Token进行额外的属性设置。
     * 默认为空实现。
     * </p>
     *
     * @param request HTTP请求对象
     * @param token   已创建的认证Token对象
     */
    public void setTokenResult(HttpServletRequest request,T token) {}

    /**
     * 将HTTP请求转换为认证对象
     * <p>
     * 根据请求中的grant_type参数判断是否匹配当前授权模式，
     * 如果匹配则解析请求参数、校验参数、创建对应的认证Token并返回。
     * </p>
     *
     * @param request HTTP请求对象
     * @return 转换后的认证Token对象，如果不匹配当前授权类型则返回null
     * @throws OAuth2AuthenticationException 当参数校验失败时抛出
     */
    @Override
    @SneakyThrows
    public Authentication convert(HttpServletRequest request) {

        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!Objects.equals(getGrantType(), grantType)) {
            return null;
        }

        // 这里目前是客户端认证信息
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        // 获取请求中的参数
        MultiValueMap<String, String> parameters = getParameters(request);

        // scope 为可选参数
        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scope) &&
                parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "OAuth 2.0 Parameter: " + OAuth2ParameterNames.SCOPE, ACCESS_TOKEN_REQUEST_ERROR_URI));
        }
        Set<String> requestedScopes = null;
        if (StringUtils.hasText(scope)) {
            requestedScopes = new HashSet<>(
                    Arrays.asList(StringUtils.delimitedListToStringArray(scope, ",")));
        }

        // 参数检查
        checkParams(request);

        // 提取附加参数（排除grant_type与client_id）
        Map<String, Object> additionalParameters = new HashMap<>((int) (parameters.size() / 0.75f) + 1);
        parameters.forEach((key, value) -> {
            if (!OAuth2ParameterNames.GRANT_TYPE.equals(key) &&
                    !OAuth2ParameterNames.CLIENT_ID.equals(key)) {
                additionalParameters.put(key, value.get(0));
            }
        });

        T token = tokenClass.getDeclaredConstructor(AuthorizationGrantType.class, Authentication.class,Set.class,Map.class).newInstance(new AuthorizationGrantType(getGrantType()), clientPrincipal, requestedScopes, additionalParameters);
        setTokenResult(request,token);

        return token;
    }

    /**
     * 从HTTP请求中提取所有参数并转换为MultiValueMap
     *
     * @param request HTTP请求对象
     * @return 包含所有请求参数的MultiValueMap
     */
    public static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            for (String value : values) {
                parameters.add(key, value);
            }
        });
        return parameters;
    }

}
