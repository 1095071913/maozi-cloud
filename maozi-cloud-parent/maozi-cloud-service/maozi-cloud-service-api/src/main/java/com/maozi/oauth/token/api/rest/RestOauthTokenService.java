package com.maozi.oauth.token.api.rest;

import com.maozi.common.constant.ApplicationNameConstant;
import com.maozi.common.constant.RemoteConstant;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.token.api.rest.fallback.RestOauthTokenServiceFallBackFactory;
import com.maozi.service.annotation.Get;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * OAuth 令牌 REST 服务接口
 * <p>
 * 通过 Feign 客户端调用 OAuth 认证服务的令牌管理接口，提供令牌内省（校验）能力。
 * 本接口保留为 Feign 兼容入口，令牌内省主链路已切换为 Dubbo RPC
 * （{@link com.maozi.oauth.token.api.rpc.RpcOauthTokenService}），新业务请直接使用 Dubbo 接口。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Tag(name = "【三方】授权令牌")
@FeignClient(value = ApplicationNameConstant.MAOZI_CLOUD_OAUTH_SERVICE, fallbackFactory = RestOauthTokenServiceFallBackFactory.class)
public interface RestOauthTokenService {

    /** 基础路径 */
	String PATH = RemoteConstant.REMOTE_PREFIX_PATH + "/oauth/token";

    /**
     * 内省令牌
     *
     * @param token 待内省的令牌字符串
     * @return 令牌内省结果，包含 active、sub、authorities 等字段，令牌无效时 active 为 false
     */
	@Get(value = PATH + "/{token}/introspect",description = "内省令牌")
	AbstractBaseResult<Map<String, Object>> restIntrospect(@PathVariable("token") String token);

}
