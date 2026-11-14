package com.maozi.system.permission.api.impl.rest;

import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.permission.api.impl.RoleServiceImpl;
import com.maozi.system.permission.api.rest.RestRoleService;
import com.maozi.system.role.param.RoleSaveUpdateParam;
import com.maozi.system.role.result.RoleInfoResult;
import com.maozi.system.role.result.RoleListResult;

import java.util.List;

/**
 * 角色REST服务实现类
 * <p>提供角色的列表、详情、新增、更新等RESTful接口，删除、状态更新及下拉列表等通用接口由基类默认实现。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestRoleServiceImpl extends BaseServiceResultImpl<RoleServiceImpl, Void> implements RestRoleService {

	/**
	 * 查询角色列表
	 *
	 * @return 角色列表视图数据
	 */
	@Override
	public AbstractBaseResult<List<RoleListResult>> listResult() {
		return ResultUtil.success(service.list(RoleListResult.class));
	}

	/**
	 * 新增角色
	 * <p>保存角色信息，并按参数同步建立角色与权限的绑定关系。</p>
	 *
	 * @param param 角色保存参数
	 * @return 新增角色的ID
	 */
	@Override
	public AbstractBaseResult<Long> saveResult(RoleSaveUpdateParam param) {
		return ResultUtil.success(service.adminSaveUpdate(null, param));
	}

	/**
	 * 查询角色详情
	 * <p>按ID查询角色详情并填充关联映射数据（如角色绑定的权限ID列表），角色不存在时抛出业务异常。</p>
	 *
	 * @param id 角色ID
	 * @return 角色详情视图数据
	 */
	@Override
	public AbstractBaseResult<RoleInfoResult> getResult(Long id) {
		return ResultUtil.success(service.getByIdThrowErrorRelation(id, RoleInfoResult.class));
	}

	/**
	 * 更新角色
	 * <p>更新角色信息，并按参数同步更新角色与权限的绑定关系。</p>
	 *
	 * @param id 角色ID
	 * @param param 角色更新参数
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> updateResult(Long id, RoleSaveUpdateParam param) {
		service.adminSaveUpdate(id, param);
		return ResultUtil.success();
	}

}
