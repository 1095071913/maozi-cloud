package com.maozi.oauth.token.api.rpc;

import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.token.param.ClientUserParam;

import java.util.List;
import java.util.Map;

/**
 * OAuth 令牌 RPC 服务接口
 * <p>
 * 提供令牌内省与授权注销（单个、批量）的 Dubbo RPC 远程调用能力。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface RpcOauthTokenService {

    /**
     * RPC方式内省令牌
     * <p>
     * 通过Dubbo RPC调用OAuth授权服务器进行令牌内省，
     * 替代HTTP调用introspection端点的方式，减少网络开销。
     * </p>
     *
     * @param token 待内省的令牌字符串
     * @return 令牌内省结果，包含 active、sub、authorities 等字段，
     *         当令牌无效时 active 为 false
     */
    AbstractBaseResult<Map<String, Object>> introspectResult(String token);

    /**
     * RPC方式注销用户令牌
     * <p>
     * 移除指定客户端下该用户的所有 OAuth2 授权记录，适用于删除用户、强制下线等场景。
     * </p>
     *
     * @param registeredClientId 已注册客户端ID（对应UserDO.clientId）
     * @param principalName      用户主体名称（用户名）
     * @return 操作结果
     */
    AbstractBaseResult<Void> destroyByPrincipalResult(Long registeredClientId, String principalName);

    /**
     * RPC方式批量注销用户令牌
     * <p>
     * 批量移除多个用户在其客户端下的所有 OAuth2 授权记录，适用于禁用角色等需统一注销多个用户令牌的场景。
     * </p>
     *
     * @param clientUsers 客户端用户参数列表，每项包含clientId和username
     * @return 操作结果
     */
    AbstractBaseResult<Void> destroyByPrincipalsResult(List<ClientUserParam> clientUsers);

}
