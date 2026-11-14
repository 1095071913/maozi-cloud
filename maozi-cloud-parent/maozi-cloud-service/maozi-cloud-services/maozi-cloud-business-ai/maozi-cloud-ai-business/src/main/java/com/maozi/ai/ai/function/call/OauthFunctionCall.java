package com.maozi.ai.ai.function.call;

import cn.hutool.core.util.RandomUtil;
import com.maozi.common.CollectionUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.client.api.rpc.RpcClientService;
import com.maozi.oauth.client.dto.ClientDTO;
import com.maozi.oauth.client.enums.AuthType;
import com.maozi.oauth.client.param.ClientSaveUpdateParam;
import com.maozi.service.api.annotation.RemoteResource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * OAuth 客户端 AI 工具调用
 * <p>
 * 供 AI 对话通过 Spring AI 工具调用（Function Call）方式操作 OAuth 客户端，
 * 工具方法标注 {@code @PreAuthorize}，执行前会进行权限校验。
 * </p>
 *
 * @author maozi
 * @date 2026/11/14
 */
@Component
public class OauthFunctionCall {

    /** OAuth 客户端 RPC 服务，用于保存客户端 */
    @RemoteResource
    private RpcClientService rpcClientService;

    /**
     * 新增客户端
     * <p>
     * 随机生成 32 位客户端密钥，默认授予密码、刷新令牌、客户端凭证三种授权类型。
     * </p>
     *
     * @param name 客户端名称
     * @param accessTokenValiditySeconds 授权令牌有效期（秒），默认 2 小时
     * @param refreshTokenValiditySeconds 刷新令牌有效期（秒），默认 7 天
     * @return 保存结果，成功时返回的客户端数据中回填明文客户端密钥
     */
    @PreAuthorize("hasAuthority('system:client:save')")
    @Tool(description = "新增客户端")
    public AbstractBaseResult<ClientDTO> saveClient(
            @ToolParam(description = "客户端名称") String name,
            @ToolParam(description = "授权令牌有效期 秒 默认2小时") Long accessTokenValiditySeconds,
            @ToolParam(description = "刷新令牌有效期 秒 默认7天") Long refreshTokenValiditySeconds
    ) {

        ClientSaveUpdateParam param = new ClientSaveUpdateParam();
        param.setName(name);
        param.setClientSecret(RandomUtil.randomString(32));
        param.setAuthorizationGrantTypes(CollectionUtil.newHashSet(AuthType.PASSWORD,AuthType.REFRESH_TOKEN,AuthType.CLIENT_CREDENTIALS));
        param.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
        param.setRefreshTokenValiditySeconds(refreshTokenValiditySeconds);

        AbstractBaseResult<ClientDTO> result = rpcClientService.aiSaveResult(param);
        if(result.isSuccess()){
            // 保存成功后将生成的明文客户端密钥回填到返回数据，供调用方查看
            result.getData().setClientSecret(param.getClientSecret());
        }
        return result;
    }

}
