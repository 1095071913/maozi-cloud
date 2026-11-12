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

package com.maozi.system.permission.param;

import com.maozi.system.permission.enums.PermissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 权限保存/更新参数
 * <p>
 * 用于新增或修改权限信息时的请求参数封装。
 * 包含权限的层级关系、基本信息、路由配置及类型等字段。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionSaveUpdateParam implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 上级权限ID，用于构建权限树形结构 */
	@Schema(description = "上级ID")
	private Long parentId;

	/** 权限名称 */
	@Schema(description = "名称")
	@NotEmpty(message = "名称不能为空")
	private String name;

	/** 权限图标 */
	@Schema(description = "图标")
	@NotEmpty(message = "图标不能为空")
	private String icon;

	/** 权限唯一标识编码 */
	@Schema(description = "标识")
	@NotEmpty(message = "标识不能为空")
	private String mark;

	/** 权限在树形结构中的深度层级 */
	@Schema(description = "深度")
	@NotNull(message = "深度不能为空")
	private Integer level;

	/** 前端路由路径 */
	@Schema(description = "路由")
	private String route;

	/** 后端服务地址URI */
	@Schema(description = "服务地址")
	private String serviceUri;

	/** 权限类型（目录/菜单/按钮） */
	@Schema(description = "类型")
	@NotNull(message = "类型不能为空")
	private PermissionType type;

	/** 排序序号，用于控制同级权限的显示顺序 */
	@Schema(description = "排序")
	private Integer sort;

}
