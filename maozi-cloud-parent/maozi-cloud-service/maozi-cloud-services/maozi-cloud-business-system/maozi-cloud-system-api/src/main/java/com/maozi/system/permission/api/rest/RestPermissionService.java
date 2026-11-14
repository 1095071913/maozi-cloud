package com.maozi.system.permission.api.rest;

import com.maozi.base.result.DropDownResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.annotation.Get;
import com.maozi.service.annotation.Post;
import com.maozi.system.permission.param.PermissionSaveUpdateParam;
import com.maozi.system.permission.result.PermissionInfoResult;
import com.maozi.system.permission.result.PermissionListResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 权限管理服务 REST 接口
 *
 * @author maozi
 * @date 2026/11/08
 */
@Tag(name = "权限模块")
public interface RestPermissionService {

	/** 基础路径常量，权限模块的统一请求路径前缀 */
	String PATH = "/permission";

	/**
	 * 获取权限列表
	 * <p>
	 * 返回平铺的权限列表（带上级 ID，可由前端组装为树形结构），按深度与排序序号降序排列。
	 * </p>
	 *
	 * @return 返回权限列表数据，包含权限 ID、名称、类型、父级 ID 等信息
	 */
	@Get(value = PATH + "/list",description = "列表")
	@PreAuthorize("hasAuthority('system:permission:list')")
	AbstractBaseResult<List<PermissionListResult>> listResult();

	/**
	 * 保存新增权限
	 *
	 * @param param 权限保存参数，包含权限名称、类型、父级 ID、排序等必要信息
	 * @return 返回新增权限的 ID
	 */
	@Post(value = PATH + "/save",description = "保存")
	@PreAuthorize("hasAuthority('system:permission:save')")
	AbstractBaseResult<Long> saveResult(@RequestBody @Valid PermissionSaveUpdateParam param);

	/**
	 * 获取权限下拉列表
	 * <p>
	 * 用于新增或更新权限时选择父级权限。
	 * </p>
	 *
	 * @return 返回权限下拉列表结果，包含权限 ID、名称、上级 ID、层级深度及类型等信息
	 */
	@Get(value = PATH + "/dropDownList",description = "下拉列表")
	@PreAuthorize("hasAuthority('system:permission:save') or hasAuthority('system:permission:update')")
	AbstractBaseResult<List<? extends DropDownResult>> dropDownListResult();





//	================== 单资源 ==================

	/** 单资源路径常量，用于指定具体权限 ID 的请求路径前缀 */
	String CURRENT_PATH = PATH + "/{id}";

	/**
	 * 获取权限详情
	 *
	 * @param id 权限 ID，用于查询指定权限的详细信息
	 * @return 返回权限详细信息，包含权限名称、类型、路径、图标、排序等完整属性
	 */
	@Get(value = CURRENT_PATH + "/get",description = "详情")
	@PreAuthorize("hasAuthority('system:permission:get')")
	AbstractBaseResult<PermissionInfoResult> getResult(@PathVariable Long id);

	/**
	 * 删除权限
	 *
	 * @param id 权限 ID，指定需要删除的权限记录
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = CURRENT_PATH + "/remove",description = "删除")
	@PreAuthorize("hasAuthority('system:permission:remove')")
	AbstractBaseResult<Void> removeByIdResult(@PathVariable Long id);

	/**
	 * 更新权限信息
	 *
	 * @param id    权限 ID，指定需要更新的权限记录
	 * @param param 权限更新参数，包含需要修改的权限属性信息
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Post(value = CURRENT_PATH + "/update",description = "更新")
	@PreAuthorize("hasAuthority('system:permission:update')")
	AbstractBaseResult<Void> saveUpdateResult(@PathVariable Long id, @RequestBody PermissionSaveUpdateParam param);

}
