package com.maozi.system.permission.result;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maozi.system.permission.enums.PermissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 权限详情视图对象
 * <p>
 * 用于展示权限的详细信息，包括层级关系、基本信息、
 * 路由配置、服务地址、权限类型及排序等。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionInfoResult implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

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

	/** 前端路由路径 */
	@Schema(description = "路由")
	private String route;

	/** 后端服务地址URI */
	@Schema(description = "服务地址")
	private String serviceUri;

	/** 权限类型，以整数形式序列化 */
	@Schema(description = "类型")
	@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
	private PermissionType type;

	/** 排序序号 */
	@Schema(description = "排序")
	private Integer sort;

}
