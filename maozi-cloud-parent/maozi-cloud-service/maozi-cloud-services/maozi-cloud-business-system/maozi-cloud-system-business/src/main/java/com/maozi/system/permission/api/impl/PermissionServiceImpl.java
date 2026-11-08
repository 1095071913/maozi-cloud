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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.permission.api.PermissionService;
import com.maozi.system.permission.api.RolePermissionService;
import com.maozi.system.permission.domain.PermissionDo;
import com.maozi.system.permission.mapper.PermissionMapper;
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
public class PermissionServiceImpl extends BaseServiceImpl<PermissionMapper,PermissionDo,Void> implements PermissionService {

	/** 资源名称，用于异常提示信息 */
	private static final String RESOURCE_NAME = "权限";

	/** 角色权限关系服务，用于校验权限是否被角色绑定 */
	@Resource(name = "rolePermissionServiceImpl")
	private RolePermissionService rolePermissionService;

	/**
	 * 获取资源名称
	 *
	 * @return 资源名称字符串
	 */
	@Override
	protected String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * 校验权限是否被其他资源绑定
	 * <p>在删除权限前校验：1.检查权限是否被角色绑定；2.检查权限是否存在子权限。</p>
	 *
	 * @param id 权限ID
	 */
	@Override
	protected void checkBind(Long id) {

		// 1.校验权限是否被角色绑定，由角色权限关系服务抛出业务异常
		rolePermissionService.checkRoleBindPermissionByPermission(id);

		// 2.校验当前权限是否存在子权限（查询 parentId = id 的权限记录），存在则禁止删除
		LambdaQueryWrapper<PermissionDo> wrapper = Wrappers.<PermissionDo>lambdaQuery().eq(PermissionDo::getParentId, id);
		ObjectUtil.checkConditionThrowError(count(wrapper) < 1, SystemErrorCode.BUSINESS_ERROR_DEFAULT_CODE, getResourceName() + "存在子权限，不允许删除");

	}

	/**
	 * 判断指定权限是否存在
	 *
	 * @param id 权限ID
	 * @return 如果权限存在返回true，否则返回false
	 */
	@Override
	public boolean has(Long id) {
		return count(Wrappers.<PermissionDo>lambdaQuery().eq(PermissionDo::getId,id)) > 0;
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

		return list(wrapper).stream().map(PermissionDo::getMark).collect(Collectors.toList());

	}

}