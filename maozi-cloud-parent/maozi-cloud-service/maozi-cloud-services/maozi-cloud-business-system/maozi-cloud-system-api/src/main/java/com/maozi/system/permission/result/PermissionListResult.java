package com.maozi.system.permission.result;

import com.maozi.system.permission.enums.PermissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 权限列表视图对象
 * <p>
 * 用于权限列表展示时的视图对象，包含权限的基本信息，
 * 如ID、上级ID、名称、图标、标识和类型等。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionListResult implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 权限ID */
	@Schema(description = "标识")
	private Long id;

	/** 上级权限ID */
	@Schema(description = "上级ID")
	private Long parentId;

	/** 权限名称 */
	@Schema(description = "名称")
	private String name;

	/** 权限图标 */
	@Schema(description = "图标")
	private String icon;

	/** 权限唯一标识编码 */
	@Schema(description = "标识")
	private String mark;

	/** 权限类型（目录/菜单/按钮） */
	@Schema(description = "类型")
	private PermissionType type;

	/** 排序序号，越大越靠前（列表按深度与排序降序返回） */
	@Schema(description = "排序")
	private Integer sort;

}
