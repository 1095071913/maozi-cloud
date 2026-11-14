package com.maozi.system.permission.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.permission.api.RolePermissionService;
import com.maozi.system.permission.api.RoleService;
import com.maozi.system.permission.api.UserRoleService;
import com.maozi.system.permission.domain.RoleDO;
import com.maozi.system.permission.mapper.RoleMapper;
import com.maozi.system.role.param.RoleSaveUpdateParam;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色服务实现类
 * <p>实现角色相关的业务逻辑，包括角色存在性判断、删除前的绑定关系校验、
 * 角色与权限的级联更新等功能。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class RoleServiceImpl extends BaseServiceImpl<RoleMapper, RoleDO> implements RoleService {

	/** 资源名称，用于异常提示信息 */
	public static final String RESOURCE_NAME = "角色";

	/** 用户角色关系服务，用于校验角色是否被用户绑定 */
	@Resource(name = "userRoleServiceImpl")
	protected UserRoleService userRoleService;

	/** 角色权限关系服务，用于管理角色与权限的绑定关系 */
	@Resource(name = "rolePermissionServiceImpl")
	protected RolePermissionService rolePermissionService;

	/** 获取资源名称 */
	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * 校验角色是否被其他资源绑定
	 * <p>删除角色、禁用角色前均会调用本校验。</p>
	 *
	 * @param id 角色ID
	 * @throws com.maozi.common.result.error.exception.BusinessResultException 角色已被用户绑定时抛出
	 */
	@Override
	public void checkBind(Long id) {
		userRoleService.checkUserBindRoleByRole(id);
	}

	/**
	 * 解除角色与所有权限的绑定关系
	 * <p>在删除角色时级联删除角色与权限的关联记录。</p>
	 *
	 * @param id 角色ID
	 */
	@Override
	public void unbind(Long id) {
		rolePermissionService.roleUnbind(id);
	}

	/**
	 * 判断指定角色是否存在
	 *
	 * @param id 角色ID
	 * @return 如果角色存在返回true，否则返回false
	 */
	@Override
	public boolean has(Long id) {
		return super.count(Wrappers.<RoleDO>lambdaQuery().eq(RoleDO::getId, id)) > 0;
	}

	/**
	 * 保存或更新角色，并同步更新角色与权限的绑定关系
	 *
	 * @param id 角色ID，新增时为null
	 * @param param 角色保存/更新参数
	 * @return 保存后的角色ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long adminSaveUpdate(Long id, RoleSaveUpdateParam param) {

		id = super.saveUpdate(id, param);

		rolePermissionService.updateBind(id, param.getBindPermissionIds(), param.getUnbindPermissionIds());

		return id;

	}

}
