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

import com.maozi.base.enums.Status;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.user.api.impl.UserServiceImpl;
import com.maozi.system.user.api.rest.RestUserService;
import com.maozi.system.user.param.UserIndividualUpdateParam;
import com.maozi.system.user.param.UserListParam;
import com.maozi.system.user.param.UserSaveUpdateParam;
import com.maozi.system.user.result.UserIndividualInfoResult;
import com.maozi.system.user.result.UserInfoResult;
import com.maozi.system.user.result.UserListResult;

/**
 * 用户REST服务实现类
 * <p>
 * 提供用户模块的 REST 接口实现，内部持有 UserServiceImpl 复用用户基础业务逻辑。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestUserServiceImpl extends BaseServiceResultImpl<UserServiceImpl, Void> implements RestUserService {

	/**
	 * 分页查询用户列表
	 * <p>关联查询并填充关联映射数据（如所属客户端）。</p>
	 *
	 * @param pageParam 分页查询参数
	 * @return 用户列表分页结果
	 */
	@Override
	public AbstractBaseResult<PageResult<UserListResult>> listResult(PageParam<UserListParam> pageParam) {
		return ResultUtil.success(service.listRelation(pageParam, UserListResult.class));
	}

	/**
	 * 新增用户
	 *
	 * @param param 用户新增参数
	 * @return 新增用户的ID
	 */
	@Override
	public AbstractBaseResult<Long> saveResult(UserSaveUpdateParam param) {
		return ResultUtil.success(service.adminSaveUpdate(null, param));
	}

	/**
	 * 删除用户
	 * <p>用户不存在时静默返回；删除时解除角色绑定，删除成功后销毁该用户全部 OAuth2 令牌，强制下线。</p>
	 *
	 * @param id 用户ID
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> removeByIdResult(Long id) {
		service.adminRemove(id);
		return ResultUtil.success();
	}

	/**
	 * 更新用户状态（启用/禁用）
	 * <p>当前状态与目标状态一致时直接返回；状态变更为禁用后销毁该用户全部 OAuth2 令牌，强制下线。</p>
	 *
	 * @param id 用户ID
	 * @param param 状态参数
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> updateStatusResult(Long id, RequestParam<Status> param) {
		service.adminUpdateStatus(id, param);
		return ResultUtil.success();
	}

	/**
	 * 查询用户详情
	 * <p>按ID查询并填充关联映射数据（如所属客户端、关联角色ID列表），用户不存在时抛出业务异常。</p>
	 *
	 * @param id 用户ID
	 * @return 用户详细信息
	 */
	@Override
	public AbstractBaseResult<UserInfoResult> getResult(Long id) {
		return ResultUtil.success(service.getByIdThrowErrorRelation(id, UserInfoResult.class));
	}

	/**
	 * 更新用户信息
	 * <p>更新时同步维护用户与角色的绑定关系；更新后状态为禁用时销毁该用户全部 OAuth2 令牌，强制下线。</p>
	 *
	 * @param id    用户ID
	 * @param param 用户更新参数
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> updateResult(Long id, UserSaveUpdateParam param) {
		service.adminSaveUpdate(id, param);
		return ResultUtil.success();
	}

	/**
	 * 获取当前登录用户的个人信息
	 * <p>从链路上下文获取当前用户ID，关联查询姓名、头像及权限标识列表。</p>
	 *
	 * @return 当前登录用户的个人信息
	 */
	@Override
	public AbstractBaseResult<UserIndividualInfoResult> individualGetResult() {
		Long userId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUserId);
		return ResultUtil.success(service.getByIdThrowErrorRelation(userId, UserIndividualInfoResult.class));
	}

	/**
	 * 更新当前登录用户的个人信息
	 * <p>修改密码时需同时传入旧密码与新密码；更新成功后销毁该用户在当前客户端下的全部 OAuth2 令牌并指示前端跳转登录页，强制重新登录。</p>
	 *
	 * @param param 个人信息更新参数（姓名、头像、旧密码、新密码）
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> individualUpdateResult(UserIndividualUpdateParam param) {
		service.adminIndividualUpdate(param);
		return ResultUtil.success();
	}

}