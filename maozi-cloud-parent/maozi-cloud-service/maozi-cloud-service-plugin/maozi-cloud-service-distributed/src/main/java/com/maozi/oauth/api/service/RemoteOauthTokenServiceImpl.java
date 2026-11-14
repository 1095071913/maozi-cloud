package com.maozi.oauth.api.service;

import com.maozi.common.ObjectUtil;
import com.maozi.oauth.token.api.OauthTokenService;
import com.maozi.oauth.token.api.rest.RestOauthTokenService;
import com.maozi.oauth.token.api.rpc.RpcOauthTokenService;
import com.maozi.oauth.token.constants.OAuth2TokenUserClaimConstants;
import com.maozi.service.api.annotation.RemoteResource;
import com.maozi.service.config.RemoteOauthTokenServiceConfiguration;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;

import java.time.Instant;
import java.util.Map;

/**
 * 资源服务器进程中的令牌内省服务实现
 * <p>
 * 根据配置模式远程调用授权服务器完成令牌内省：模式为空或 rpc 时走 Dubbo RPC
 * {@link RpcOauthTokenService}，其余走 REST（Feign）{@link RestOauthTokenService}，
 * 供 {@code OpaqueTokenIntrospector} 使用；由 {@link RemoteOauthTokenServiceConfiguration}
 * 在无本地 {@link OauthTokenService} 实现时注册。
 * </p>
 *
 * @author maozi
 * @date 2026/6/26 08:12
 */
public class RemoteOauthTokenServiceImpl implements OauthTokenService {

    /** 内省调用模式：RPC（Dubbo） */
    private static final String MODE_RPC = "rpc";

    /** 内省调用模式：rpc 走 Dubbo，其余走 REST；未配置时视为 rpc */
    @Value("${spring.security.oauth2.resourceserver.opaquetoken.mode:}")
    private String mode;

    /** Dubbo 远程令牌服务引用 */
    @RemoteResource
    private RpcOauthTokenService rpcOauthTokenService;

    /** REST 远程令牌服务引用（Feign） */
    @Resource
    private RestOauthTokenService restOauthTokenService;

    /**
     * 远程内省令牌
     * <p>
     * 模式为空或 {@code rpc} 时走 Dubbo RPC，否则走 REST（Feign）调用；
     * 内省失败（结果为错误）时直接抛出业务异常。
     * </p>
     *
     * @param token 访问令牌
     * @return 令牌内省声明信息
     */
    @Override
    public Map<String, Object> introspect(String token) {

        if(ObjectUtil.isNotNullEmpty(mode) && !MODE_RPC.equalsIgnoreCase(mode)){

            // REST 返回的时间与用户 ID 为字符串，还原为内省器所需的 Instant/Long 类型
            Map<String, Object> introspectResultData = restOauthTokenService.introspectResult(token).getResultDataThrowError();

            Object iatObj = introspectResultData.get(OAuth2TokenIntrospectionClaimNames.IAT);
            if(ObjectUtil.isNotNullEmpty(iatObj)){
                introspectResultData.put(OAuth2TokenIntrospectionClaimNames.IAT, Instant.parse(iatObj.toString()));
            }

            Object expObj = introspectResultData.get(OAuth2TokenIntrospectionClaimNames.EXP);
            if(ObjectUtil.isNotNullEmpty(expObj)){
                introspectResultData.put(OAuth2TokenIntrospectionClaimNames.EXP, Instant.parse(expObj.toString()));
            }

            Object nbfObj = introspectResultData.get(OAuth2TokenIntrospectionClaimNames.NBF);
            if(ObjectUtil.isNotNullEmpty(nbfObj)){
                introspectResultData.put(OAuth2TokenIntrospectionClaimNames.NBF, Instant.parse(nbfObj.toString()));
            }

            Object userIdObj = introspectResultData.get(OAuth2TokenUserClaimConstants.USER_ID);
            if(ObjectUtil.isNotNullEmpty(userIdObj)){
                introspectResultData.put(OAuth2TokenUserClaimConstants.USER_ID, Long.parseLong(userIdObj.toString()));
            }

            return introspectResultData;

        }else{
            return rpcOauthTokenService.introspectResult(token).getResultDataThrowError();
        }

    }

}
