package com.maozi.system.user.api.impl.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.system.user.api.impl.UserServiceImpl;
import com.maozi.system.user.api.rpc.RpcUserService;
import com.maozi.system.user.domain.UserDo;
import com.maozi.system.user.result.AiGetUserInfoResult;
import com.maozi.system.user.result.OauthUserInfoResult;

import java.util.List;

/**
 * 用户RPC服务实现类
 * <p>
 * 提供用户模块的Dubbo RPC远程调用接口实现，包括根据用户名获取 OAuth 认证用户信息
 * （用户ID、加密密码及全部权限标识），以及根据用户名和指定角色ID获取该角色下的
 * 权限标识列表等功能。
 * 继承自 UserServiceImpl，复用用户基础业务逻辑。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcUserServiceImpl extends UserServiceImpl implements RpcUserService {

	/**
	 * 根据用户名获取 OAuth 认证所需的用户信息
	 * <p>
	 * 供授权服务器进行密码模式认证与权限填充；
	 * 用户名为空或用户不存在时抛出业务异常。
	 * </p>
	 *
	 * @param username 用户名
	 * @return OAuth 认证用户信息（含 ID、密码、权限标识列表）
	 */
	@Override
	public AbstractBaseResult<OauthUserInfoResult> rpcGetOauthUserInfoByUsername(String username) {

		// 用户名为空时抛出数据不存在业务异常
		ObjectUtil.isNullEmptyThrowError(username, getResourceName());

		LambdaQueryWrapper<UserDo> wrapper = Wrappers.lambdaQuery();

		// 仅查询用户ID和密码字段
		wrapper.select(UserDo::getId,UserDo::getPassword);
		wrapper.eq(UserDo::getUsername,username);

		UserDo domain = getOne(wrapper);

		// 用户不存在时抛出业务异常
		ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

		Long userId = domain.getId();
		return ResultUtil.success(new OauthUserInfoResult(userId,domain.getPassword(),getPermissionsByUserId(userId)));

	}

	/**
	 * 根据用户名和指定角色ID获取该角色下的权限标识列表
	 * <p>
	 * 先验证用户是否绑定了指定角色，如果未绑定则抛出异常。
	 * 然后查询指定角色关联的权限标识列表。
	 * </p>
	 *
	 * @param username 用户名
	 * @param roleId   角色ID
	 * @return 指定角色下的权限标识列表
	 */
	@Override
	public AbstractBaseResult<List<String>> rpcGetPermissionsByUsernameRole(String username, Long roleId) {

		Long id = getAvailableByUsername(username);

		// 校验用户绑定了指定角色，未绑定则抛出异常
		userRoleService.checkUserBindRole(id,roleId);

		List<String> responses = CollectionUtil.newArrayList();

		// 查询角色关联的权限ID集合，并转换为权限标识列表
		List<Long> permissionIds = rolePermissionService.getPermissionsByRole(roleId);

		List<String> marks = permissionService.getMarks(permissionIds);

        responses.addAll(marks);

		return ResultUtil.success(responses);

	}

	/**
	 * 根据用户ID查询AI工具所需的用户信息
	 * <p>
	 * 携带关联数据映射转换为 {@link AiGetUserInfoResult}，
	 * 自动填充所属客户端信息及该用户的权限标识列表，用户不存在时抛出业务异常。
	 * </p>
	 *
	 * @param userId 用户ID
	 * @return AI工具用户信息封装的统一结果
	 */
	@Override
	public AbstractBaseResult<AiGetUserInfoResult> rpcAiGetUserInfo(Long userId) {
		return ResultUtil.success(getByIdThrowErrorRelation(userId,AiGetUserInfoResult.class));
	}

}