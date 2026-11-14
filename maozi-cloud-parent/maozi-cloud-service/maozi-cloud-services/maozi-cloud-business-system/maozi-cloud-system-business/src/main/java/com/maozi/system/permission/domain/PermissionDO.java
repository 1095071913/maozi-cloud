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
import com.maozi.db.domain.AbstractBaseNameDomain;
import com.maozi.system.permission.enums.PermissionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 权限实体类
 * <p>对应数据库表 system_permission，存储系统权限的详细信息，
 * 包括权限的层级结构、图标、标识、路由、类型等属性。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("system_permission")
public class PermissionDO extends AbstractBaseNameDomain {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 父权限ID，用于构建权限树形结构 */
	private Long parentId;

	/** 权限图标 */
	private String icon;

	/** 权限标识，用于权限匹配的唯一编码 */
	private String mark;

	/** 权限深度（层级），表示在树形结构中的层级 */
	private Integer level;

	/** 前端路由路径 */
	private String route;

	/** 后端服务请求地址 */
	private String serviceUri;

	/** 权限类型（目录、菜单、按钮） */
	private PermissionType type;

	/** 排序序号，值越大越靠前 */
	private Integer sort;

}
