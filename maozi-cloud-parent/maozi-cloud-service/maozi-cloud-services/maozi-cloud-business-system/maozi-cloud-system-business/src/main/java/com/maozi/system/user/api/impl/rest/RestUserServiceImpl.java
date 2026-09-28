/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.maozi.system.user.api.impl.rest;

import cn.hutool.extra.cglib.CglibUtil;
import com.maozi.base.enums.Status;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.service.api.annotation.RestService;
import com.maozi.system.redirect.enums.RedirectType;
import com.maozi.system.redirect.util.RedirectUtil;
import com.maozi.system.user.api.impl.UserServiceImpl;
import com.maozi.system.user.api.rest.RestUserService;
import com.maozi.system.user.domain.UserDo;
import com.maozi.system.user.param.UserIndividualUpdateParam;
import com.maozi.system.user.param.UserListParam;
import com.maozi.system.user.param.UserSaveUpdateParam;
import com.maozi.system.user.result.UserIndividualInfoResult;
import com.maozi.system.user.result.UserInfoResult;
import com.maozi.system.user.result.UserListResult;

/**
 * 用户REST服务实现类
 * <p>
 * 提供用户模块的RESTful API接口实现，包括用户的分页查询、新增、删除、
 * 详情查询、更新、状态变更以及当前登录用户个人信息查询等功能。
 * 继承自 UserServiceImpl，复用用户基础业务逻辑。
 * </p>
 *
 * @author maozi
 */
@RestService
public class RestUserServiceImpl extends UserServiceImpl implements RestUserService {

	/**
	 * 分页查询用户列表
	 *
	 * @param pageParam 分页查询参数，包含分页信息和用户列表查询条件
	 * @return 分页查询结果，包含用户列表数据
	 */
	@Override
	public AbstractBaseResult<PageResult<UserListResult>> restList(PageParam<UserListParam> pageParam) {
		return ResultUtil.success(listRelation(pageParam, UserListResult.class));
	}

	/**
	 * 新增用户
	 *
	 * @param param 用户新增参数，包含用户基本信息和角色绑定信息
	 * @return 新增用户的ID
	 */
	@Override
	public AbstractBaseResult<Long> restSave(UserSaveUpdateParam param) {
		return ResultUtil.success(restSaveUpdate(null,param));
	}

	/**
	 * 删除用户
	 *
	 * @param id 需要删除的用户ID
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restRemove(Long id) {

		// 先获取用户的用户名和客户端ID，删除后需用于注销认证令牌
		UserDo user = getById(id, UserDo::getUsername,UserDo::getClientId);
		if(ObjectUtil.isNullEmpty(user)){
			return ResultUtil.success();
		}

		// 删除用户数据库记录
		AbstractBaseResult<Void> responseResult = removeByIdResult(id);

		// 通过RPC调用OAuth服务，根据clientId+用户名销毁该用户的所有OAuth2授权令牌
		rpcOauthTokenService.rpcDestroyByPrincipal(user.getClientId(), user.getUsername()).getResultDataThrowError();

		return responseResult;

	}

	/**
	 * 更新用户状态（启用/禁用）
	 * <p>委托本类重写的 {@code updateStatusResult} 处理：当前状态与目标状态一致时直接返回成功；
	 * 状态变更为禁用后会销毁该用户的全部OAuth2令牌，强制其下线。</p>
	 *
	 * @param id 需要更新状态的用户ID
	 * @param param 状态参数，包含目标状态值
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restUpdateStatusResult(Long id, RequestParam<Status> param) {
		return updateStatusResult(id, param);
	}

	/**
	 * 查询用户详情
	 * <p>按ID查询用户详情并填充关联映射数据（如所属客户端、关联角色ID列表），用户不存在时抛出业务异常。</p>
	 *
	 * @param id 用户ID
	 * @return 用户详细信息
	 */
	@Override
	public AbstractBaseResult<UserInfoResult> restGet(Long id) {
		return ResultUtil.success(getByIdThrowErrorRelation(id, UserInfoResult.class));
	}

	/**
	 * 更新用户信息
	 * <p>
	 * 若更新参数中的状态为禁用，更新完成后会远程销毁该用户的全部 OAuth2 令牌，强制其重新登录。
	 * </p>
	 *
	 * @param id    需要更新的用户ID
	 * @param param 用户更新参数，包含需要更新的用户信息
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restUpdate(Long id, UserSaveUpdateParam param) {

		restSaveUpdate(id,param);

		if(Status.DISABLE == param.getStatus()){
			// 更新后状态为禁用时，销毁该用户的全部OAuth2令牌，强制下线
			UserDo user = getByIdThrowError(id,UserDo::getUsername,UserDo::getClientId);
			rpcOauthTokenService.rpcDestroyByPrincipal(user.getClientId(), user.getUsername()).getResultDataThrowError();
		}

		return ResultUtil.success();

	}

	/**
	 * 更新用户状态（启用/禁用）
	 * <p>
	 * 如果用户当前状态与目标状态一致，则不做任何操作直接返回成功；
	 * 状态变更为禁用后，会远程销毁该用户的全部 OAuth2 令牌，强制其重新登录。
	 * </p>
	 *
	 * @param id    需要更新状态的用户ID
	 * @param param 状态参数，包含目标状态值
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> updateStatusResult(Long id, RequestParam<Status> param){

		Status status = param.getData();

		UserDo user = getByIdThrowError(id,UserDo::getUsername,UserDo::getClientId,UserDo::getStatus);

		// 当前状态与目标状态一致时，无需更新，直接返回成功
		if(user.getStatus() == status){
			return ResultUtil.success();
		}

		user.setId(id);
		user.setStatus(status);
		updateById(user);

		// 禁用用户后销毁其全部OAuth2令牌，强制下线
		if(Status.DISABLE == status){
			rpcOauthTokenService.rpcDestroyByPrincipal(user.getClientId(), user.getUsername()).getResultDataThrowError();
		}

		return ResultUtil.success();

	}

	/**
	 * 获取当前登录用户的个人信息
	 * <p>
	 * 从应用上下文中获取当前登录用户的用户ID，按关联映射查询并返回该用户的
	 * 姓名、头像以及权限标识列表信息。
	 * </p>
	 *
	 * @return 当前登录用户的个人信息
	 */
	@Override
	public AbstractBaseResult<UserIndividualInfoResult> restIndividualGet() {
		return ResultUtil.success(getByIdThrowErrorRelation(ApplicationLinkContext.getCurrentUserInfo(CurrentUserInfo::getUserId), UserIndividualInfoResult.class));
	}

	/**
	 * 更新当前登录用户的个人信息
	 * <p>
	 * 旧密码与新密码同时传入时先校验旧密码，校验通过后加密新密码一并更新；
	 * 两者均未传入时不修改密码，仅更新其他基本信息。更新成功后销毁该用户在该客户端下的全部
	 * OAuth2 令牌强制重新登录，并设置 {@code X-Redirect} 响应头指示前端跳转登录页。
	 * </p>
	 *
	 * @param param 个人信息更新参数（姓名、头像、旧密码、新密码）
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restIndividualUpdate(UserIndividualUpdateParam param) {

		Long userId = ApplicationLinkContext.getCurrentUserInfo(CurrentUserInfo::getUserId);

		String paramPassword = param.getPassword();
		String paramNewPassword = param.getNewPassword();

		// 旧密码与新密码同时传入时，先校验旧密码，再将新密码加密
		if(ObjectUtil.isNotNullEmpty(paramPassword) && ObjectUtil.isNotNullEmpty(paramNewPassword)){
			UserDo domain = getByIdThrowError(userId, UserDo::getPassword);

			// 旧密码与库中密文不匹配时抛出业务异常
			if(!passwordEncoder.matches(paramPassword,domain.getPassword())){
				throw new BusinessResultException("旧密码不正确");
			}

			paramNewPassword = passwordEncoder.encode(paramNewPassword);
		}

		UserDo domain = CglibUtil.copy(param, UserDo.class);
		domain.setId(userId);
		domain.setPassword(paramNewPassword);
		updateById(domain);

		// 更新成功后销毁当前用户在该客户端下的全部OAuth2令牌，并指示前端跳转登录页，强制重新登录
		Long clientId = ApplicationLinkContext.getCurrentUserInfo(CurrentUserInfo::getClientId);
		String username = ApplicationLinkContext.getCurrentUserInfo(CurrentUserInfo::getUsername);
		rpcOauthTokenService.rpcDestroyByPrincipal(clientId, username).getResultDataThrowError();
		RedirectUtil.redirect(RedirectType.LOGIN);

		return ResultUtil.success();

	}

}