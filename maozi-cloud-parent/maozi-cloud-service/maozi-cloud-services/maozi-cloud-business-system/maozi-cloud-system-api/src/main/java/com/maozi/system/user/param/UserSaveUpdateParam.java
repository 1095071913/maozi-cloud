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

package com.maozi.system.user.param;

import com.maozi.base.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户保存/更新参数
 * <p>
 * 用于新增或修改用户信息时的请求参数封装。
 * 包含用户的基本信息、所属客户端以及需要绑定/解绑的角色列表。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSaveUpdateParam implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 用户登录账号 */
	@NotEmpty(message = "账号不能为空")
	@Schema(description = "账号")
	private String username;

	/** 用户姓名 */
	@NotEmpty(message = "名称不能为空")
	@Schema(description = "名称")
	private String name;

	/** 用户登录密码 */
	@NotEmpty(message = "密码不能为空")
	@Schema(description = "密码")
	private String password;

	/** 所属客户端ID */
	@NotNull(message = "客户端不能为空")
	@Schema(description = "客户端")
	private Long clientId;

	/** 用户头像图标 */
	@NotEmpty(message = "图标不能为空")
	@Schema(description = "图标")
	private String icon;

	/** 用户状态（启用/禁用） */
	@NotNull(message = "状态不能为空")
	@Schema(description = "状态")
	private Status status;

	/** 需要绑定的角色ID列表 */
	@Schema(description = "绑定角色列表")
	private List<Long> bindRoleIds;

	/** 需要解绑的角色ID列表 */
	@Schema(description = "解绑角色列表")
	private List<Long> unbindRoleIds;

}
