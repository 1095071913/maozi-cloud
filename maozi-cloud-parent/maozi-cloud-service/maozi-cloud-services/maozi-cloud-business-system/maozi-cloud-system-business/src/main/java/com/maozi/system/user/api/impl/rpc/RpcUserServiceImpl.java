package com.maozi.system.user.api.impl.rpc;

import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.user.api.impl.UserServiceImpl;
import com.maozi.system.user.api.rpc.RpcUserService;
import com.maozi.system.user.result.AiGetUserInfoResult;
import com.maozi.system.user.result.OauthUserInfoResult;

/**
 * 用户RPC服务实现类
 * <p>
 * 提供用户模块的 Dubbo RPC 远程调用接口实现，内部持有 UserServiceImpl 复用用户基础业务逻辑。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcUserServiceImpl extends BaseServiceResultImpl<UserServiceImpl, Void> implements RpcUserService {

	/**
	 * 根据用户名获取 OAuth 认证所需的用户信息
	 * <p>供授权服务器加载用户详情进行认证与权限填充；用户名为空或用户不存在时抛出业务异常。</p>
	 *
	 * @param username 用户名
	 * @return OAuth 认证用户信息（含 ID、密码、权限标识列表）
	 */
	@Override
	public AbstractBaseResult<OauthUserInfoResult> getOauthUserInfoByUsernameResult(String username) {
		return ResultUtil.success(service.getOauthUserInfoByUsername(username));
	}

	/**
	 * 根据用户ID查询AI工具所需的用户信息
	 * <p>自动填充所属客户端信息及该用户的权限标识列表；用户不存在时抛出业务异常。</p>
	 *
	 * @param userId 用户ID
	 * @return AI工具用户信息封装的统一结果
	 */
	@Override
	public AbstractBaseResult<AiGetUserInfoResult> aiGetUserInfoResult(Long userId) {
		return ResultUtil.success(service.getByIdThrowErrorRelation(userId, AiGetUserInfoResult.class));
	}

}