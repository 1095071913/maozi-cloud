package com.maozi.system.role.result;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maozi.base.enums.Status;
import com.maozi.base.plugin.mapping.QueryMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色详情视图对象
 * <p>
 * 用于展示角色的详细信息，包括角色名称、描述、状态以及关联的权限列表。
 * 通过 QueryMapping 注解自动查询角色关联的权限ID列表。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleInfoResult implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 角色ID，在JSON序列化时隐藏 */
	@JsonIgnore
	@Schema(hidden = true)
	private Long id;

	/** 角色名称 */
	@Schema(description = "名称")
	private String name;

	/** 角色描述 */
	@Schema(description = "描述")
	private String description;

	/** 角色状态，以整数形式序列化 */
	@Schema(description = "状态")
	@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
	private Status status;

	/** 角色关联的权限ID列表，通过角色权限关系服务查询自动填充 */
	@Schema(description = "权限列表",ref = "StringArrayList")
	@QueryMapping(isService = true,serviceName = "rolePermissionServiceImpl",functionName = "getPermissionsByRole",relationField = "id")
	private List<Long> permissionIds;

}
