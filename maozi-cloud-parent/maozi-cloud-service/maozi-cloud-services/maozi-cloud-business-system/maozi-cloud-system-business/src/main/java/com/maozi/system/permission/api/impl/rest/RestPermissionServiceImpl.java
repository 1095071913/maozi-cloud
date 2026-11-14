package com.maozi.system.permission.api.impl.rest;

import com.maozi.base.result.DropDownResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.permission.api.impl.PermissionServiceImpl;
import com.maozi.system.permission.api.rest.RestPermissionService;
import com.maozi.system.permission.param.PermissionSaveUpdateParam;
import com.maozi.system.permission.result.PermissionDropDownResult;
import com.maozi.system.permission.result.PermissionInfoResult;
import com.maozi.system.permission.result.PermissionListResult;

import java.util.List;

/**
 * 权限REST服务实现类
 * <p>提供权限的列表、详情、新增、更新及下拉列表等RESTful接口，删除等通用接口由基类默认实现。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestPermissionServiceImpl extends BaseServiceResultImpl<PermissionServiceImpl, Void> implements RestPermissionService {

	/**
	 * 查询所有权限的列表数据
	 *
	 * @return 权限列表视图数据
	 */
	@Override
	public AbstractBaseResult<List<PermissionListResult>> listResult() {
		return ResultUtil.success(service.adminList());
	}

	/**
	 * 新增权限
	 *
	 * @param param 权限保存参数
	 * @return 新增权限的ID
	 */
	@Override
	public AbstractBaseResult<Long> saveResult(PermissionSaveUpdateParam param) {
		return ResultUtil.success(service.saveUpdate(null, param));
	}

	/**
	 * 查询权限下拉列表
	 * <p>区别于基类通用下拉仅返回ID和名称，此处额外返回父ID、深度、类型、排序字段，用于构建权限树。</p>
	 *
	 * @return 权限下拉选项列表
	 */
	@Override
	public AbstractBaseResult<List<? extends DropDownResult>> dropDownListResult() {
		return ResultUtil.success(service.list(PermissionDropDownResult.class));
	}

	/**
	 * 查询权限详情
	 *
	 * @param id 权限ID
	 * @return 权限详情视图数据
	 */
	@Override
	public AbstractBaseResult<PermissionInfoResult> getResult(Long id) {
		return ResultUtil.success(service.getByIdThrowError(id, PermissionInfoResult.class));
	}

	/**
	 * 更新权限
	 *
	 * @param id 权限ID
	 * @param param 权限更新参数
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> saveUpdateResult(Long id, PermissionSaveUpdateParam param) {
		service.saveUpdate(id, param);
		return ResultUtil.success();
	}

}
