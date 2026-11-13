package com.maozi.system.permission.api.rest;

import com.maozi.base.enums.Status;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.DropDownResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.annotation.Get;
import com.maozi.service.annotation.Post;
import com.maozi.system.role.param.RoleSaveUpdateParam;
import com.maozi.system.role.result.RoleInfoResult;
import com.maozi.system.role.result.RoleListResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 角色管理服务 REST 接口
 * <p>
 * 提供系统角色的 RESTful API 接口定义，
 * 包括角色列表查询、角色新增、角色详情查询、角色删除、角色更新、
 * 角色状态更新以及角色下拉列表等操作。
 * 所有接口均需要相应的权限授权才能访问。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Tag(name = "角色模块")
public interface RestRoleService {

	/** 基础路径常量，角色模块的统一请求路径前缀 */
	String PATH = "/role";

	/**
	 * 获取角色列表
	 *
	 * @return 返回角色列表数据，仅包含角色 ID 与名称
	 */
	@Get(value = PATH + "/list",description = "列表")
	@PreAuthorize("hasAuthority('system:role:list')")
	AbstractBaseResult<List<RoleListResult>> restList();

	/**
	 * 保存新增角色
	 *
	 * @param param 角色保存参数，包含角色名称、描述、关联权限等必要信息
	 * @return 返回新增角色的 ID
	 */
	@Post(value = PATH + "/save",description = "保存")
	@PreAuthorize("hasAuthority('system:role:save')")
	AbstractBaseResult<Long> restSave(@RequestBody @Valid RoleSaveUpdateParam param);

	/**
	 * 获取角色下拉列表
	 * <p>
	 * 用于新增或更新用户时选择角色的下拉列表数据。
	 * </p>
	 *
	 * @return 返回角色下拉列表结果，包含角色 ID 和名称信息
	 */
	@Get(value = PATH + "/dropDownList",description = "下拉列表")
	@PreAuthorize("hasAuthority('system:role:save') or hasAuthority('system:role:update')")
	AbstractBaseResult<List<DropDownResult>> restDropDownListResult();





//	===================== 单资源 =======================

	/** 单资源路径常量，用于指定具体角色 ID 的请求路径前缀 */
	String CURRENT_PATH = PATH + "/{id}";

	/**
	 * 获取角色详情
	 *
	 * @param id 角色 ID，用于查询指定角色的详细信息
	 * @return 返回角色详细信息，包含角色名称、描述、关联权限等完整属性
	 */
	@Get(value = CURRENT_PATH + "/get",description = "详情")
	@PreAuthorize("hasAuthority('system:role:get')")
	AbstractBaseResult<RoleInfoResult> restGet(@PathVariable Long id);

	/**
	 * 删除角色
	 *
	 * @param id 角色 ID，指定需要删除的角色记录
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = CURRENT_PATH + "/remove",description = "删除")
	@PreAuthorize("hasAuthority('system:role:remove')")
	AbstractBaseResult<Void> removeByIdResult(@PathVariable Long id);

	/**
	 * 更新角色信息
	 *
	 * @param id    角色 ID，指定需要更新的角色记录
	 * @param param 角色更新参数，包含需要修改的角色属性信息
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = CURRENT_PATH + "/update",description = "更新")
	@PreAuthorize("hasAuthority('system:role:update')")
	AbstractBaseResult<Void> restUpdate(@PathVariable Long id, @RequestBody RoleSaveUpdateParam param);

	/**
	 * 更新角色状态
	 *
	 * @param id    角色 ID，指定需要更新状态的角色记录
	 * @param param 状态参数，包含需要更新的目标状态值（启用/禁用）
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = CURRENT_PATH + "/updateStatus",description = "更新状态")
	@PreAuthorize("hasAuthority('system:role:update')")
	AbstractBaseResult<Void> restUpdateStatusResult(@PathVariable Long id, @RequestBody RequestParam<Status> param);

}
