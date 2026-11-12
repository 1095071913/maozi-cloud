package com.maozi.oauth.token.api.impl.rpc;

import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.token.api.impl.OauthTokenServiceImpl;
import com.maozi.oauth.token.api.rpc.RpcOauthTokenService;
import com.maozi.oauth.token.param.ClientUserParam;
import com.maozi.service.api.annotation.RemoteService;

import java.util.List;
import java.util.Map;

/**
 * OAuth令牌Dubbo RPC服务实现类
 * <p>
 * 通过Dubbo RPC对外暴露令牌内省与授权注销能力：资源服务器可直接通过RPC调用
 * 进行令牌校验（替代HTTP调用introspection端点的方式，减少网络开销），
 * 也可按用户单个或批量注销其在客户端下的授权记录，实现强制下线。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcOauthTokenServiceImpl extends OauthTokenServiceImpl implements RpcOauthTokenService {

    /**
     * RPC令牌内省接口
     * <p>
     * 根据传入的AccessToken值查找对应的授权记录，验证令牌是否有效，
     * 并返回令牌的声明信息（用户名、客户端ID、权限列表、授权范围等）。
     * 如果令牌无效或已过期，返回active=false。
     * </p>
     *
     * @param token 待内省的访问令牌值
     * @return 包含令牌声明信息的Map，至少包含active字段标识令牌是否有效
     */
    @Override
    public AbstractBaseResult<Map<String, Object>> rpcIntrospect(String token) {
        return ResultUtil.success(introspect(token));
    }

    /**
     * RPC根据客户端ID和用户名注销授权
     * <p>
     * 移除指定客户端下指定用户的所有OAuth2授权记录，实现单用户注销功能。
     * </p>
     *
     * @param registeredClientId 已注册客户端ID
     * @param principalName      用户主体名称（用户名）
     * @return 无数据的统一响应结果
     */
    @Override
    public AbstractBaseResult<Void> rpcDestroyByPrincipal(Long registeredClientId, String principalName) {
        // 根据clientId+用户名移除该用户在该客户端下的所有OAuth2授权
        authorizationService.removeAllByPrincipal(registeredClientId, principalName);
        return ResultUtil.success();
    }

    /**
     * RPC批量注销多个用户的授权
     * <p>
     * 批量移除多个用户的所有OAuth2授权记录，内部通过MGET+批量DEL减少Redis网络往返，
     * 适用于角色禁用等需要同时注销多个用户的场景。
     * </p>
     *
     * @param clientUsers 客户端用户参数列表，每项包含clientId和username
     * @return 无数据的统一响应结果
     */
    @Override
    public AbstractBaseResult<Void> rpcDestroyByPrincipals(List<ClientUserParam> clientUsers) {
        // 批量移除多个用户的所有OAuth2授权，内部通过MGET+批量DEL减少Redis网络往返
        authorizationService.removeAllByPrincipals(clientUsers);
        return ResultUtil.success();
    }

}
