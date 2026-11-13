/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.maozi.system.permission.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.permission.api.PermissionService;
import com.maozi.system.permission.api.RolePermissionService;
import com.maozi.system.permission.domain.PermissionDo;
import com.maozi.system.permission.mapper.PermissionMapper;
import com.maozi.system.permission.result.PermissionListResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 权限服务实现类
 * <p>实现权限相关的业务逻辑，包括权限存在性判断、权限标识查询、
 * 删除前的绑定关系校验等功能。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class PermissionServiceImpl extends BaseServiceImpl<PermissionMapper, PermissionDo> implements PermissionService {

	/** 资源名称，用于异常提示信息 */
	public static final String RESOURCE_NAME = "权限";

	/** 角色权限关系服务，用于校验权限是否被角色绑定 */
	@Resource(name = "rolePermissionServiceImpl")
	protected RolePermissionService rolePermissionService;

	/**
	 * 获取资源名称
	 *
	 * @return 资源名称字符串
	 */
	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * 删除权限前校验绑定关系与子权限
	 * <p>权限已被角色绑定或存在子权限时抛出业务异常。</p>
	 *
	 * @param id 权限ID
	 */
	@Override
	public void checkBind(Long id) {

		// 1.校验权限是否被角色绑定，由角色权限关系服务抛出业务异常
		rolePermissionService.checkRoleBindPermissionByPermission(id);

		// 2.校验当前权限是否存在子权限（查询 parentId = id 的权限记录），存在则禁止删除
		LambdaQueryWrapper<PermissionDo> wrapper = Wrappers.<PermissionDo>lambdaQuery().eq(PermissionDo::getParentId, id);
		ObjectUtil.checkConditionThrowError(super.count(wrapper) < 1, SystemErrorCode.BUSINESS_ERROR_DEFAULT_CODE, getResourceName() + "存在子权限，不允许删除");

	}

	/**
	 * 判断指定权限是否存在
	 *
	 * @param id 权限ID
	 * @return 如果权限存在返回true，否则返回false
	 */
	@Override
	public boolean has(Long id) {
		return super.count(Wrappers.<PermissionDo>lambdaQuery().eq(PermissionDo::getId,id)) > 0;
	}

	/**
	 * 根据权限ID集合获取权限标识列表
	 *
	 * @param ids 权限ID集合
	 * @return 权限标识（mark）列表
	 */
	@Override
	public List<String> getMarks(Collection<Long> ids) {

		LambdaQueryWrapper<PermissionDo> wrapper = Wrappers.lambdaQuery();

		wrapper.select(PermissionDo::getMark);

		wrapper.in(PermissionDo::getId,ids);

		return super.list(wrapper).stream().map(PermissionDo::getMark).collect(Collectors.toList());

	}

	/**
	 * 查询所有权限的列表数据，按权限深度、排序序号降序排列
	 *
	 * @return 权限列表视图数据
	 */
	public List<PermissionListResult> adminList() {

		QueryWrapper<PermissionDo> wrapper = Wrappers.query();

		// 仅查询列表视图对象所需的字段列
		wrapper.select(super.getColumns(PermissionListResult.class,false));

		// 按权限深度、排序序号降序排列
		wrapper.orderByDesc(CollectionUtil.newArrayList(super.getColumns(PermissionDo::getLevel,PermissionDo::getSort)));

		return super.list(wrapper, PermissionListResult::new);

	}

}