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
 */
package com.maozi.system.user.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maozi.db.domain.AbstractBaseNameDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 用户实体类
 * <p>
 * 对应数据库表 system_user，继承自 AbstractBaseNameDomain（包含ID、名称、状态、逻辑删除标志、创建时间等基础字段）。
 * 用于存储系统用户的核心信息，包括用户账号、密码、头像图标以及所属客户端ID。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@TableName("system_user")
public class UserDo extends AbstractBaseNameDomain {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 用户账号，用于系统登录的唯一标识 */
	private String username;

	/** 用户密码，加密存储 */
	private String password;

	/** 用户头像图标地址 */
	private String icon;

	/** 所属客户端ID，关联客户端表 */
	private Long clientId;

}
