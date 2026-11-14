package com.maozi.oauth.token.api.impl.rest;

import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.token.api.impl.OauthTokenServiceImpl;
import com.maozi.oauth.token.api.rest.RestOauthTokenService;
import com.maozi.service.api.annotation.RestService;

import java.util.Map;

/**
 * OAuth 令牌服务 REST 实现层
 * <p>
 * 继承 {@link OauthTokenServiceImpl} 复用本地内省逻辑，以 REST 接口对外提供令牌内省能力，供资源服务器通过 HTTP 调用。
 *
 * @author maozi
 * @date 2026/7/8 17:15
 */
@RestService
public class RestOauthTokenServiceImpl extends OauthTokenServiceImpl implements RestOauthTokenService {

    /**
     * REST 令牌内省，返回统一结果封装
     */
    @Override
    public AbstractBaseResult<Map<String, Object>> introspectResult(String token) {
        return ResultUtil.success(super.introspect(token));
    }

}
