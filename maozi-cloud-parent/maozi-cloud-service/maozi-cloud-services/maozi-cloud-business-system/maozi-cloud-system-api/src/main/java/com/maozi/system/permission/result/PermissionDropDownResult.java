package com.maozi.system.permission.result;

import com.maozi.base.result.DropDownResult;
import com.maozi.system.permission.enums.PermissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 权限下拉选择结果
 * <p>
 * 用于权限下拉列表选择时返回的结果对象。
 * 继承自通用下拉结果基类（提供 ID 与名称选项字段），
 * 额外包含上级权限ID、层级深度、类型及排序信息，
 * 以支持树形结构的权限选择。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PermissionDropDownResult extends DropDownResult {

	/** 上级权限ID，用于构建权限树形结构 */
	@Schema(description = "上级ID")
	private Long parentId;

	/** 权限在树形结构中的层级深度 */
	@Schema(description = "深度")
	private Integer level;

	/** 权限类型（目录/菜单/按钮） */
	@Schema(description = "类型")
	private PermissionType type;

	/** 排序序号，用于控制同级权限的显示顺序 */
	@Schema(description = "排序")
	private Integer sort;

}
