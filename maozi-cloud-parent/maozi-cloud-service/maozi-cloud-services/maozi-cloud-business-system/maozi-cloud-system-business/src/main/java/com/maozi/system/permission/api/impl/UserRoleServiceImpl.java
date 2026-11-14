package com.maozi.system.permission.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.permission.api.RoleService;
import com.maozi.system.permission.api.UserRoleService;
import com.maozi.system.permission.domain.UserRoleDO;
import com.maozi.system.permission.mapper.UserRoleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 用户角色关系服务实现类
 * <p>实现用户与角色绑定关系的管理逻辑，包括绑定关系校验、
 * 批量绑定/解绑、根据用户或角色查询关联关系以及用户解绑等功能。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class UserRoleServiceImpl extends BaseServiceImpl<UserRoleMapper, UserRoleDO> implements UserRoleService {

	/** 资源名称，用于异常提示信息 */
	public static final String RESOURCE_NAME = "用户角色关系";

	/** 角色服务，用于校验角色是否存在 */
	@Resource(name = "roleServiceImpl")
	protected RoleService roleService;

	/** 获取资源名称 */
	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * 检查指定角色是否已被用户绑定
	 *
	 * @param roleId 角色ID
	 * @throws com.maozi.common.result.error.exception.BusinessResultException 角色已被用户绑定时抛出
	 */
	@Override
	public void checkUserBindRoleByRole(Long roleId) {
		LambdaQueryWrapper<UserRoleDO> wrapper = Wrappers.<UserRoleDO>lambdaQuery().eq(UserRoleDO::getRoleId,roleId);
		ObjectUtil.checkConditionThrowError(super.count(wrapper) < 1, SystemErrorCode.BUSINESS_ERROR_DEFAULT_CODE,"【角色】已被用户绑定");
	}

	/**
	 * 更新用户与角色的绑定关系
	 * <p>批量绑定：对每个待绑定角色ID，校验角色存在且尚未绑定后进行保存；
	 * 批量解绑：对每个待解绑角色ID，直接删除对应的绑定记录。</p>
	 *
	 * @param userId 用户ID
	 * @param bindRoleIds 需要绑定的角色ID列表
	 * @param unbindRoleIds 需要解绑的角色ID列表
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateBind(Long userId, List<Long> bindRoleIds, List<Long> unbindRoleIds) {

		if(ObjectUtil.isNotNullEmpty(bindRoleIds)) {

			Consumer<Long> consumer = (roleId)->{

				UserRoleDO domainSave = UserRoleDO.builder().userId(userId).roleId(roleId).build();
				// 绑定关系不存在且角色存在时才保存，避免重复绑定
				if(super.count(Wrappers.lambdaQuery(domainSave)) < 1 && roleService.has(roleId)) {
					super.save(domainSave);
				}

			};

			bindRoleIds.forEach(consumer);

		}

		if(ObjectUtil.isNotNullEmpty(unbindRoleIds)) {

			Consumer<Long> consumer = (roleId) -> super.remove(
			    Wrappers.lambdaQuery(UserRoleDO.builder().userId(userId).roleId(roleId).build()
			));

			unbindRoleIds.forEach(consumer);

		}

	}

	/**
	 * 根据用户ID获取关联的角色ID列表
	 *
	 * @param userId 用户ID
	 * @return 角色ID列表
	 */
	@Override
	public List<Long> getRolesByUser(Long userId) {

		LambdaQueryWrapper<UserRoleDO> wrapper = Wrappers.lambdaQuery();
		wrapper.select(UserRoleDO::getRoleId);
		wrapper.eq(UserRoleDO::getUserId,userId);

		return super.list(wrapper).stream().map(UserRoleDO::getRoleId).collect(Collectors.toList());

	}

	/**
	 * 检查指定用户是否绑定了指定角色
	 *
	 * @param userId 用户ID
	 * @param roleId 角色ID
	 * @throws com.maozi.common.result.error.exception.BusinessResultException 用户未绑定该角色时抛出
	 */
	@Override
	public void checkUserBindRole(Long userId, Long roleId) {

		LambdaQueryWrapper<UserRoleDO> wrapper = Wrappers.lambdaQuery();
		wrapper.eq(UserRoleDO::getUserId,userId);
		wrapper.eq(UserRoleDO::getRoleId,roleId);

		ObjectUtil.checkConditionThrowError(super.count(wrapper) > 0,SystemErrorCode.BUSINESS_ERROR_DEFAULT_CODE,"【用户】未绑定该角色");
	}

	/**
	 * 根据角色ID获取关联的用户ID列表
	 *
	 * @param roleId 角色ID
	 * @return 用户ID列表
	 */
	@Override
	public List<Long> getUsersByRole(Long roleId) {

		LambdaQueryWrapper<UserRoleDO> wrapper = Wrappers.lambdaQuery(UserRoleDO.builder().roleId(roleId).build());

		wrapper.select(UserRoleDO::getUserId);
		wrapper.eq(UserRoleDO::getRoleId,roleId);

		return super.list(wrapper).stream().map(UserRoleDO::getUserId).collect(Collectors.toList());

	}

	/**
	 * 解除用户与所有角色的绑定关系
	 * <p>删除指定用户关联的所有用户角色记录。</p>
	 *
	 * @param userId 用户ID
	 */
	@Override
	public void userUnbind(Long userId) {
		super.remove(Wrappers.lambdaQuery(UserRoleDO.builder().userId(userId).build()));
	}

}