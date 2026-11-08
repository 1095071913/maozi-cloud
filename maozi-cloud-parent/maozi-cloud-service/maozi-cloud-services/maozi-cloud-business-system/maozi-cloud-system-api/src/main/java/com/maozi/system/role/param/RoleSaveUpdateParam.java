package com.maozi.system.role.param;

import com.maozi.base.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 角色保存/更新参数
 * <p>
 * 用于新增或修改角色信息时的请求参数封装。
 * 包含角色的基本信息以及需要绑定/解绑的权限ID列表。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleSaveUpdateParam implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 角色名称 */
	@Schema(description = "名称")
	private String name;

	/** 角色描述 */
	@Schema(description = "描述")
	private String description;

	/** 角色状态（启用/禁用） */
	@Schema(description = "状态")
	private Status status;

	/** 需要绑定的权限ID集合 */
	@Schema(description = "绑定权限列表")
	private Set<Long> bindPermissionIds;

	/** 需要解绑的权限ID集合 */
	@Schema(description = "解绑权限列表")
	private Set<Long> unbindPermissionIds;

}
