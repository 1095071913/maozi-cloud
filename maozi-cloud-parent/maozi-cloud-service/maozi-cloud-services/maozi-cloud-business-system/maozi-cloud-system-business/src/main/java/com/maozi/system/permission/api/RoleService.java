package com.maozi.system.permission.api;

/**
 * 角色服务接口
 * <p>提供角色相关的业务操作，包括角色存在性判断等功能。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface RoleService {

	/**
	 * 判断指定角色是否存在
	 *
	 * @param id 角色ID
	 * @return 如果角色存在返回true，否则返回false
	 */
	boolean has(Long id);

}
