package com.maozi.system.permission.api.impl.rest;

import com.maozi.base.enums.Status;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.DropDownResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.permission.api.impl.RoleServiceImpl;
import com.maozi.system.permission.api.rest.RestRoleService;
import com.maozi.system.permission.domain.RoleDo;
import com.maozi.system.role.param.RoleSaveUpdateParam;
import com.maozi.system.role.result.RoleInfoResult;
import com.maozi.system.role.result.RoleListResult;

import java.util.List;

/**
 * 角色REST服务实现类
 * <p>提供角色管理的RESTful接口实现，包括角色列表查询、
 * 角色新增、角色详情查询、角色更新、角色删除、角色状态更新以及角色下拉列表等功能。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestRoleServiceImpl extends BaseServiceResultImpl<RoleServiceImpl, Void> implements RestRoleService {

	/**
	 * 查询角色列表
	 * <p>仅查询角色的ID和名称两列数据。</p>
	 *
	 * @return 角色列表视图数据
	 */
	@Override
	public AbstractBaseResult<List<RoleListResult>> restList() {
		return ResultUtil.success(service.list(RoleListResult.class, RoleDo::getId,RoleDo::getName));
	}

	/**
	 * 新增角色
	 *
	 * @param param 角色保存参数
	 * @return 新增角色的ID
	 */
	@Override
	public AbstractBaseResult<Long> restSave(RoleSaveUpdateParam param) {
		return ResultUtil.success(service.restSaveUpdate(null,param));
	}

	/**
	 * 查询角色下拉列表
	 * <p>复用基类通用下拉查询，仅返回启用状态角色的ID和名称。</p>
	 *
	 * @return 角色下拉选项列表
	 */
	@Override
	public AbstractBaseResult<List<DropDownResult>> restDropDownListResult() {
		return dropDownListResult();
	}

	/**
	 * 查询角色详情
	 * <p>按ID查询角色详情并填充关联映射数据（如角色绑定的权限ID列表），角色不存在时抛出业务异常。</p>
	 *
	 * @param id 角色ID
	 * @return 角色详情视图数据
	 */
	@Override
	public AbstractBaseResult<RoleInfoResult> restGet(Long id) {
		return ResultUtil.success(service.getByIdThrowErrorRelation(id, RoleInfoResult.class));
	}

	/**
	 * 删除角色
	 * <p>删除前校验角色未被用户绑定，并级联解除该角色与权限的关联关系。</p>
	 *
	 * @param id 角色ID
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restRemove(Long id) {
		return removeByIdResult(id);
	}

	/**
	 * 更新角色
	 *
	 * @param id 角色ID
	 * @param param 角色更新参数
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restUpdate(Long id, RoleSaveUpdateParam param) {

		service.restSaveUpdate(id, param);

		return ResultUtil.success();

	}

	/**
	 * 更新角色状态（启用/禁用）
	 * <p>委托基类 {@code updateStatusResult} 仅更新状态字段；
	 * 目标状态为禁用时会先校验角色是否已被用户绑定，已被绑定时抛出业务异常。</p>
	 *
	 * @param id 角色ID
	 * @param param 状态参数，包含目标状态值
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restUpdateStatusResult(Long id, RequestParam<Status> param) {
		return updateStatusResult(id, param);
	}

}
