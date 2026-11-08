package com.maozi.system.permission.api.impl.rest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.system.permission.api.impl.PermissionServiceImpl;
import com.maozi.system.permission.api.rest.RestPermissionService;
import com.maozi.system.permission.domain.PermissionDo;
import com.maozi.system.permission.param.PermissionSaveUpdateParam;
import com.maozi.system.permission.result.PermissionDropDownResult;
import com.maozi.system.permission.result.PermissionInfoResult;
import com.maozi.system.permission.result.PermissionListResult;

import java.util.List;

/**
 * 权限REST服务实现类
 * <p>提供权限管理的RESTful接口实现，包括权限列表查询、
 * 权限新增、权限详情查询、权限更新、权限删除以及下拉列表等功能。</p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestPermissionServiceImpl extends PermissionServiceImpl implements RestPermissionService {

	/**
	 * 查询权限列表
	 * <p>按权限深度和排序降序返回所有权限的列表视图数据。</p>
	 *
	 * @return 权限列表视图数据
	 */
	@Override
	public AbstractBaseResult<List<PermissionListResult>> restList() {

		QueryWrapper<PermissionDo> wrapper = Wrappers.query();

		// 仅查询列表视图对象所需的字段列
		wrapper.select(getColumns(PermissionListResult.class,false));

		// 按权限深度、排序序号降序排列
		wrapper.orderByDesc(CollectionUtil.newArrayList(getColumns(PermissionDo::getLevel,PermissionDo::getSort)));

		return ResultUtil.success(list(wrapper, PermissionListResult::new));

	}

	/**
	 * 新增权限
	 *
	 * @param param 权限保存参数
	 * @return 新增权限的ID
	 */
	@Override
	public AbstractBaseResult<Long> restSave(PermissionSaveUpdateParam param) {
		return saveUpdateResult(null,param);
	}

	/**
	 * 查询权限下拉列表
	 * <p>区别于基类通用下拉（仅ID和名称），此处直接指定权限下拉所需的查询列，
	 * 返回包含ID、父ID、深度、名称和类型的下拉选项数据。</p>
	 *
	 * @return 权限下拉选项列表
	 */
	@Override
	public AbstractBaseResult<List<PermissionDropDownResult>> restDropDownListResult() {
		return ResultUtil.success(list(PermissionDropDownResult.class, PermissionDo::getId, PermissionDo::getParentId, PermissionDo::getLevel, PermissionDo::getName, PermissionDo::getType));
	}

	/**
	 * 查询权限详情
	 *
	 * @param id 权限ID
	 * @return 权限详情视图数据
	 */
	@Override
	public AbstractBaseResult<PermissionInfoResult> restGet(Long id) {
		return ResultUtil.success(getByIdThrowError(id, PermissionInfoResult.class, PermissionDo::getParentId,PermissionDo::getName,PermissionDo::getIcon,PermissionDo::getMark,PermissionDo::getRoute,PermissionDo::getServiceUri,PermissionDo::getType,PermissionDo::getSort));
	}

	/**
	 * 删除权限
	 *
	 * @param id 权限ID
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restRemove(Long id) {
		return removeByIdResult(id);
	}

	/**
	 * 更新权限
	 *
	 * @param id 权限ID
	 * @param param 权限更新参数
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restUpdate(Long id, PermissionSaveUpdateParam param) {

		saveUpdate(id,param);

		return ResultUtil.success();

	}

}
