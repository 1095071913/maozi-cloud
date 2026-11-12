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

package com.maozi.system.permission.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * 角色权限关系服务接口
 * <p>提供角色与权限绑定关系的管理功能，包括绑定关系校验、
 * 绑定/解绑操作以及根据角色查询权限等功能。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface RolePermissionService {

	/**
	 * 检查指定权限是否已被角色绑定
	 * <p>如果权限已被角色绑定，则抛出业务异常。</p>
	 *
	 * @param permissionId 权限ID
	 */
	void checkRoleBindPermissionByPermission(Long permissionId);

	/**
	 * 更新角色与权限的绑定关系
	 * <p>批量绑定和解绑角色与权限的关联关系。</p>
	 *
	 * @param roleId 角色ID
	 * @param bindPermissionIds 需要绑定的权限ID集合
	 * @param unbindPermissionIds 需要解绑的权限ID集合
	 */
	void updateBind(Long roleId, Set<Long> bindPermissionIds, Set<Long> unbindPermissionIds);

	/**
	 * 根据多个角色ID获取关联的权限ID集合
	 *
	 * @param roleIds 角色ID列表
	 * @return 权限ID集合（去重）
	 */
	Collection<Long> getPermissionsByRoles(List<Long> roleIds);

	/**
	 * 根据单个角色ID获取关联的权限ID列表
	 *
	 * @param roleId 角色ID
	 * @return 权限ID列表
	 */
	List<Long> getPermissionsByRole(Long roleId);

	/**
	 * 解除角色与所有权限的绑定关系
	 *
	 * @param roleId 角色ID
	 */
	void roleUnbind(Long roleId);

}
