package com.maozi.system.permission.api;

import java.util.List;

/**
 * 用户角色关系服务接口
 * <p>提供用户与角色绑定关系的管理功能，包括绑定关系校验、
 * 绑定/解绑操作以及根据用户或角色查询关联关系等功能。</p>
 *
 * @author maozi
 */
public interface UserRoleService {

	/**
	 * 检查指定角色是否已被用户绑定
	 * <p>如果角色已被用户绑定，则抛出业务异常。</p>
	 *
	 * @param roleId 角色ID
	 */
	void checkUserBindRoleByRole(Long roleId);

	/**
	 * 更新用户与角色的绑定关系
	 * <p>批量绑定和解绑用户与角色的关联关系。</p>
	 *
	 * @param userId 用户ID
	 * @param bindRoleIds 需要绑定的角色ID列表
	 * @param unbindRoleIds 需要解绑的角色ID列表
	 */
	void updateBind(Long userId,List<Long> bindRoleIds,List<Long> unbindRoleIds);

	/**
	 * 根据用户ID获取关联的角色ID列表
	 *
	 * @param userId 用户ID
	 * @return 角色ID列表
	 */
	List<Long> getRolesByUser(Long userId);

	/**
	 * 根据角色ID获取关联的用户ID列表
	 *
	 * @param roleId 角色ID
	 * @return 用户ID列表
	 */
	List<Long> getUsersByRole(Long roleId);

	/**
	 * 检查指定用户是否绑定了指定角色
	 * <p>如果用户未绑定该角色，则抛出业务异常。</p>
	 *
	 * @param userId 用户ID
	 * @param roleId 角色ID
	 */
	void checkUserBindRole(Long userId,Long roleId);

	/**
	 * 解除用户与所有角色的绑定关系
	 *
	 * @param userId 用户ID
	 */
	void userUnbind(Long userId);

}