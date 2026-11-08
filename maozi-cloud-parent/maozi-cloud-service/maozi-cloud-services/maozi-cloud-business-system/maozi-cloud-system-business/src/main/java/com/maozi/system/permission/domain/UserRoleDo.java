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
package com.maozi.system.permission.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maozi.db.domain.AbstractBaseDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 用户角色关系实体类
 * <p>对应数据库表 system_user_role，存储用户与角色的多对多关联关系，
 * 一个用户可以拥有多个角色，一个角色也可以分配给多个用户。</p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@TableName("system_user_role")
public class UserRoleDo extends AbstractBaseDomain {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 角色ID，关联 system_role 表 */
	private Long roleId;

	/** 用户ID，关联用户表 */
	private Long userId;

}
