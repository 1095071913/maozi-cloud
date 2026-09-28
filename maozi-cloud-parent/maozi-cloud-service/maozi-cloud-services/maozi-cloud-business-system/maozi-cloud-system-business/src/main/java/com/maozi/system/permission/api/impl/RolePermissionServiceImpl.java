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
import com.maozi.system.permission.domain.RolePermissionDo;
import com.maozi.system.permission.mapper.RolePermissionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 角色权限关系服务实现类
 * <p>实现角色与权限绑定关系的管理逻辑，包括绑定关系校验、
 * 批量绑定/解绑、根据角色查询权限以及角色解绑等功能。</p>
 *
 * @author maozi
 */
@Service
public class RolePermissionServiceImpl extends BaseServiceImpl<RolePermissionMapper,RolePermissionDo,Void> implements RolePermissionService {

	/** 资源名称，用于异常提示信息 */
	private final static String RESOURCE_NAME = "角色权限关系";

	/** 权限服务，用于校验权限是否存在 */
	@Resource(name = "permissionServiceImpl")
	private PermissionService permissionService;

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
	 * 检查指定权限是否已被角色绑定
	 * <p>如果权限已被角色绑定，则抛出业务异常。</p>
	 *
	 * @param permissionId 权限ID
	 */
	@Override
	public void checkRoleBindPermissionByPermission(Long permissionId) {
		LambdaQueryWrapper<RolePermissionDo> wrapper = Wrappers.lambdaQuery(RolePermissionDo.builder().permissionId(permissionId).build());
		ObjectUtil.checkConditionThrowError(count(wrapper) < 1,SystemErrorCode.BUSINESS_ERROR_DEFAULT_CODE,"【权限】已被角色绑定");
	}

	/**
	 * 更新角色与权限的绑定关系
	 * <p>批量绑定：对每个待绑定权限ID，校验权限存在且尚未绑定后进行保存；
	 * 批量解绑：对每个待解绑权限ID，直接删除对应的绑定记录。</p>
	 *
	 * @param roleId 角色ID
	 * @param bindPermissionIds 需要绑定的权限ID集合
	 * @param unbindPermissionIds 需要解绑的权限ID集合
	 */
	@Override
	public void updateBind(Long roleId, Set<Long> bindPermissionIds, Set<Long> unbindPermissionIds) {

		if(ObjectUtil.isNotNullEmpty(bindPermissionIds)) {

			Consumer<Long> consumer = (permissionId)->{

				RolePermissionDo domainSave = RolePermissionDo.builder().roleId(roleId).permissionId(permissionId).build();

				// 绑定关系不存在且权限存在时才保存，避免重复绑定
				if(count(Wrappers.lambdaQuery(domainSave)) < 1 && permissionService.has(permissionId)) {
					save(domainSave);
				}

			};

			bindPermissionIds.forEach(Objects.requireNonNull(consumer));

		}

		if(ObjectUtil.isNotNullEmpty(unbindPermissionIds)) {

			Consumer<Long> consumer = (permissionId)->{
				// 直接删除对应的角色权限绑定记录
				remove(Wrappers.lambdaQuery(RolePermissionDo.builder().roleId(roleId).permissionId(permissionId).build()));
			};

			unbindPermissionIds.forEach(Objects.requireNonNull(consumer));

		}

	}

	/**
	 * 根据多个角色ID获取关联的权限ID集合（去重）
	 *
	 * @param roleIds 角色ID列表
	 * @return 权限ID集合
	 */
	@Override
	public Collection<Long> getPermissionsByRoles(List<Long> roleIds) {

		LambdaQueryWrapper<RolePermissionDo> wrapper = Wrappers.lambdaQuery();

		wrapper.select(RolePermissionDo::getPermissionId);

		wrapper.in(RolePermissionDo::getRoleId, roleIds);

		return list(wrapper).stream().map(RolePermissionDo::getPermissionId).collect(Collectors.toSet());

	}

	/**
	 * 根据单个角色ID获取关联的权限ID列表
	 *
	 * @param roleId 角色ID
	 * @return 权限ID列表
	 */
	@Override
	public List<Long> getPermissionsByRole(Long roleId) {

		LambdaQueryWrapper<RolePermissionDo> wrapper = Wrappers.lambdaQuery();

		wrapper.select(RolePermissionDo::getPermissionId);

		wrapper.eq(RolePermissionDo::getRoleId, roleId);

		return list(wrapper).stream().map(RolePermissionDo::getPermissionId).collect(Collectors.toList());

	}

	/**
	 * 解除角色与所有权限的绑定关系
	 * <p>删除指定角色关联的所有角色权限记录。</p>
	 *
	 * @param roleId 角色ID
	 */
	@Override
	public void roleUnbind(Long roleId) {
		remove(Wrappers.lambdaQuery(RolePermissionDo.builder().roleId(roleId).build()));
	}

}