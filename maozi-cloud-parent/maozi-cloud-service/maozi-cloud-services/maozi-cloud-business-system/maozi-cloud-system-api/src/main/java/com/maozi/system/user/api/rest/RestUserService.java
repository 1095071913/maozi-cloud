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

package com.maozi.system.user.api.rest;

import com.maozi.base.enums.Status;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.annotation.Get;
import com.maozi.service.annotation.Post;
import com.maozi.system.user.param.UserIndividualUpdateParam;
import com.maozi.system.user.param.UserListParam;
import com.maozi.system.user.param.UserSaveUpdateParam;
import com.maozi.system.user.result.UserIndividualInfoResult;
import com.maozi.system.user.result.UserInfoResult;
import com.maozi.system.user.result.UserListResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 用户管理服务 REST 接口
 * <p>
 * 除用户增删改查外，还提供基于当前登录认证信息的个人详情查询与个人信息更新接口。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Tag(name = "用户模块")
public interface RestUserService {

	/** 基础路径常量，用户模块的统一请求路径前缀 */
	String PATH = "/user";

	/**
	 * 获取用户分页列表
	 *
	 * @param pageParam 分页查询参数，包含页码、每页数量以及用户搜索条件（如用户名称）
	 * @return 返回用户分页列表数据，包含用户 ID、姓名、所属客户端、状态、创建时间等信息
	 */
	@Post(value = PATH + "/list",description = "列表")
	@PreAuthorize("hasAuthority('system:user:list')")
	AbstractBaseResult<PageResult<UserListResult>> listResult(@RequestBody PageParam<UserListParam> pageParam);

	/**
	 * 保存新增用户
	 *
	 * @param param 用户保存参数，包含账号、密码、姓名、关联角色等必要信息
	 * @return 返回新增用户的 ID
	 */
	@Post(value = PATH + "/save",description = "保存")
	@PreAuthorize("hasAuthority('system:user:save')")
	AbstractBaseResult<Long> saveResult(@RequestBody UserSaveUpdateParam param);





//	================== 指定资源 ===================

	/** 单资源路径常量，用于指定具体用户 ID 的请求路径前缀 */
	String CURRENT_PATH = PATH + "/{id}";

	/**
	 * 获取用户详情
	 *
	 * @param id 用户 ID，用于查询指定用户的详细信息
	 * @return 返回用户详细信息，包含账号、姓名、所属客户端、状态、关联角色等完整属性
	 */
	@Get(value = CURRENT_PATH + "/get",description = "详情")
	@PreAuthorize("hasAuthority('system:user:get')")
	AbstractBaseResult<UserInfoResult> getResult(@PathVariable Long id);

	/**
	 * 删除用户
	 *
	 * @param id 用户 ID，指定需要删除的用户记录
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = CURRENT_PATH + "/remove",description = "删除")
	@PreAuthorize("hasAuthority('system:user:remove')")
	AbstractBaseResult<Void> removeByIdResult(@PathVariable Long id);

	/**
	 * 更新用户状态（启用/禁用）
	 *
	 * @param id    用户 ID，指定需要更新状态的用户记录
	 * @param param 状态参数，包含需要更新的目标状态值（启用/禁用）
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = CURRENT_PATH + "/updateStatus",description = "更新状态")
	@PreAuthorize("hasAuthority('system:user:update')")
	AbstractBaseResult<Void> updateStatusResult(@PathVariable Long id, @RequestBody RequestParam<Status> param);

	/**
	 * 更新用户信息
	 *
	 * @param id    用户 ID，指定需要更新的用户记录
	 * @param param 用户更新参数，包含需要修改的用户属性信息
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = CURRENT_PATH + "/update",description = "更新")
	@PreAuthorize("hasAuthority('system:user:update')")
	AbstractBaseResult<Void> updateResult(@PathVariable Long id, @RequestBody UserSaveUpdateParam param);





//	====================== 个人 =========================

	/** 个人信息路径常量，用于当前登录用户个人信息相关的请求路径前缀 */
	String INDIVIDUAL_PATH = PATH + "/individual";

	/**
	 * 获取当前登录用户的个人详情
	 * <p>
	 * 无需传入用户 ID，系统会根据当前登录的认证信息自动获取对应用户的详细信息。
	 * </p>
	 *
	 * @return 返回当前登录用户的个人信息，包含姓名、头像、权限标识等个人相关数据
	 */
	@Get(value = INDIVIDUAL_PATH + "/get",description = "个人详情")
	AbstractBaseResult<UserIndividualInfoResult> individualGetResult();

	/**
	 * 更新当前登录用户的个人信息
	 * <p>
	 * 无需传入用户 ID，系统会根据当前登录的认证信息自动定位对应用户并更新其个人信息。
	 * </p>
	 *
	 * @param param 个人信息更新参数，包含需要修改的个人属性（如姓名、头像、密码等）
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = INDIVIDUAL_PATH + "/update",description = "个人信息更新")
	AbstractBaseResult<Void> individualUpdateResult(@RequestBody UserIndividualUpdateParam param);

}
