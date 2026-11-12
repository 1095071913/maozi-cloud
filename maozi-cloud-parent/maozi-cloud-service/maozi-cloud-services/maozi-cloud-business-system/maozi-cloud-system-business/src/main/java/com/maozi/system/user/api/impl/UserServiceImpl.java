
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

package com.maozi.system.user.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.oauth.client.api.rpc.RpcClientService;
import com.maozi.oauth.token.api.rpc.RpcOauthTokenService;
import com.maozi.oauth.token.param.ClientUserParam;
import com.maozi.service.api.annotation.RemoteResource;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.permission.api.PermissionService;
import com.maozi.system.permission.api.RolePermissionService;
import com.maozi.system.permission.api.UserRoleService;
import com.maozi.system.user.api.UserService;
import com.maozi.system.user.domain.UserDo;
import com.maozi.system.user.mapper.UserMapper;
import com.maozi.system.user.param.UserSaveUpdateParam;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 用户服务实现类
 * <p>
 * 用户模块的核心业务实现，继承自 BaseServiceImpl，实现了 UserService 接口。
 * 提供用户的查询、新增、更新、删除、权限获取等基础业务逻辑。
 * 同时提供 REST 和 RPC 服务实现的公共基础方法。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, UserDo> implements UserService {

    /** 资源名称常量，用于异常提示信息 */
    public static final String RESOURCE_NAME = "用户";

    /**
     * 密码编码器（DelegatingPasswordEncoder 委派模式，默认 {bcrypt} 算法且密文带算法前缀），
     * 用于用户密码的加密及旧密码比对校验
     */
    public final static PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    /**
     * 获取资源名称
     *
     * @return 资源名称字符串
     */
    @Override
    public String getResourceName() {
        return RESOURCE_NAME;
    }

    /** 用户角色服务，用于管理用户与角色的关联关系 */
    @Resource(name = "userRoleServiceImpl")
    public UserRoleService userRoleService;

    /** 角色权限服务，用于查询角色关联的权限信息 */
    @Resource(name = "rolePermissionServiceImpl")
    public RolePermissionService rolePermissionService;

    /** 权限服务，用于查询权限标识信息 */
    @Resource(name = "permissionServiceImpl")
    public PermissionService permissionService;

    /** OAuth令牌RPC服务，用于远程调用认证令牌相关接口 */
    @RemoteResource
    public RpcOauthTokenService rpcOauthTokenService;

    /** 客户端RPC服务，用于远程调用客户端相关接口 */
    @RemoteResource
    public RpcClientService rpcClientService;

    /**
     * 根据用户名获取可用的用户ID
     * <p>
     * 查询指定用户名对应的可用（未被禁用/删除）用户记录，并返回其ID。
     * 如果用户名为空或未查询到可用用户则抛出异常。
     * </p>
     *
     * @param username 用户名
     * @return 可用用户的ID
     */
    public Long getAvailableByUsername(String username) {

        ObjectUtil.isNullEmptyThrowError(username, getResourceName());

        MPJLambdaWrapper<UserDo> wrapper = MPJWrappers.lambdaJoin();

        wrapper.select(UserDo::getId);
        wrapper.eq(UserDo::getUsername,username);

        UserDo domain = getAvailableByParam(wrapper);

        return domain.getId();

    }

    /**
     * 用户新增或更新的统一处理方法（REST层调用）
     * <p>
     * 当 id 为空时执行新增操作，当 id 不为空时执行更新操作。
     * 新增时（客户端ID与用户名均非空的前提下）会校验客户端是否可用
     * 以及用户名在对应客户端下是否重复。
     * 更新时不允许修改用户名和客户端ID。
     * 密码不为空时会进行加密处理。
     * 操作完成后同步更新用户的角色绑定关系。
     * </p>
     *
     * @param id    用户ID，新增时为空，更新时为已有用户ID
     * @param param 用户新增/更新参数
     * @return 新增或更新后的用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long restSaveUpdate(Long id, UserSaveUpdateParam param) {

        if(ObjectUtil.isNotNullEmpty(id)) {

            // 更新时不允许修改用户名和客户端ID，置空以忽略这两个字段
            param.setUsername(null);

            param.setClientId(null);

        }else {

            if(ObjectUtil.isNotNullEmpty(param.getClientId()) && ObjectUtil.isNotNullEmpty(param.getUsername())) {

                // 校验客户端存在且可用，不可用则抛出异常
                rpcClientService.checkAvailableResult(param.getClientId()).getResultDataThrowError();

                // 校验同一客户端下用户名未被占用，已存在则抛出异常
                checkNotHas(MPJWrappers.lambdaJoin(UserDo.builder()
                        .clientId(param.getClientId())
                        .username(param.getUsername())
                        .build()));

            }

        }

        if(ObjectUtil.isNotNullEmpty(param.getPassword())) {
            // 密码非空时先加密再入库
            param.setPassword(PASSWORD_ENCODER.encode(param.getPassword()));
        }

        id = saveUpdate(id,param);

        // 保存后同步更新用户与角色的绑定关系
        userRoleService.updateBind(id, param.getBindRoleIds(), param.getUnbindRoleIds());

        return id;

    }

    /**
     * 获取当前登录用户的权限列表
     * <p>
     * 从Spring Security上下文中获取当前认证用户的权限标识列表。
     * </p>
     *
     * @return 当前用户拥有的权限标识列表
     */
    protected List<String> getCurrentPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    /**
     * 根据用户名获取该用户的权限标识列表
     * <p>
     * 先通过用户名查询用户ID，再查询该用户关联的角色ID列表，
     * 然后通过角色ID查询关联的权限ID集合，最后获取权限标识列表。
     * </p>
     *
     * @param username 用户名
     * @return 该用户拥有的权限标识列表
     */
    protected List<String> getPermissions(String username) {

        Long userId = getAvailableByUsername(username);

        return getPermissionsByUserId(userId);

    }

    /**
     * 根据用户ID获取该用户的权限标识列表
     * <p>
     * 先查询用户绑定的角色ID列表，再通过角色查询关联的权限ID集合，
     * 最后转换为权限标识列表；用户未绑定角色时返回空列表。
     * </p>
     *
     * @param userId 用户ID
     * @return 该用户拥有的权限标识列表
     */
    public List<String> getPermissionsByUserId(Long userId) {

        List<String> responses = CollectionUtil.newArrayList();

        List<Long> roleIds = userRoleService.getRolesByUser(userId);

        if(!roleIds.isEmpty()) {

            Collection<Long> permissionIds = rolePermissionService.getPermissionsByRoles(roleIds);

            List<String> marks = permissionService.getMarks(permissionIds);

            responses.addAll(marks);

        }

        return responses;

    }

    /**
     * 根据用户ID获取客户端用户信息
     * <p>
     * 查询指定用户ID对应的用户名和客户端ID，映射为 ClientUserParam 返回。
     * 如果未查询到数据则抛出异常。
     * </p>
     *
     * @param id 用户ID
     * @return 包含用户名和客户端ID的客户端用户信息对象
     */
    @Override
    public ClientUserParam getClientUser(Long id) {
        return getByIdThrowError(id,ClientUserParam.class,UserDo::getUsername,UserDo::getClientId);
    }

    /**
     * 根据用户ID列表批量获取客户端用户信息
     * <p>
     * 根据传入的用户ID列表查询对应的用户名和客户端ID信息。
     * 如果ID列表为空则抛出异常。
     * </p>
     *
     * @param ids 用户ID列表
     * @return 客户端用户信息参数对象列表
     */
    @Override
    public List<ClientUserParam> getClientUsers(List<Long> ids) {

        CollectionUtil.collectionIsEmptyThrowError(ids, getResourceName() + "列表");

        LambdaQueryWrapper<UserDo> wrapper = Wrappers.lambdaQuery();

        wrapper.select(UserDo::getUsername,UserDo::getClientId);

        wrapper.in(UserDo::getId, ids);

        return list(wrapper, ClientUserParam::new);

    }

    /**
     * 解绑用户的所有角色关联关系
     *
     * @param id 需要解绑角色的用户ID
     */
    @Override
    public void unbind(Long id) {
        userRoleService.userUnbind(id);
    }

}
