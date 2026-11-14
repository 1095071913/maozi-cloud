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
 * 通过Dubbo RPC对外暴露令牌内省与授权注销能力，资源服务器可直接通过RPC调用完成令牌校验；
 * 支持按用户单个或批量注销授权记录，实现强制下线。
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcOauthTokenServiceImpl extends OauthTokenServiceImpl implements RpcOauthTokenService {

    /**
     * RPC 令牌内省，返回统一结果封装
     */
    @Override
    public AbstractBaseResult<Map<String, Object>> introspectResult(String token) {
        return ResultUtil.success(super.introspect(token));
    }

    /**
     * 注销指定客户端下指定用户的所有OAuth2授权记录
     *
     * @param registeredClientId 已注册客户端ID
     * @param principalName      用户主体名称
     * @return 无数据的统一响应结果
     */
    @Override
    public AbstractBaseResult<Void> destroyByPrincipalResult(Long registeredClientId, String principalName) {
        authorizationService.removeAllByPrincipal(registeredClientId, principalName);
        return ResultUtil.success();
    }

    /**
     * 批量注销多个用户的授权记录，内部通过MGET+批量DEL减少Redis网络往返
     *
     * @param clientUsers 客户端用户参数列表，每项包含clientId和username
     * @return 无数据的统一响应结果
     */
    @Override
    public AbstractBaseResult<Void> destroyByPrincipalsResult(List<ClientUserParam> clientUsers) {
        authorizationService.removeAllByPrincipals(clientUsers);
        return ResultUtil.success();
    }

}
