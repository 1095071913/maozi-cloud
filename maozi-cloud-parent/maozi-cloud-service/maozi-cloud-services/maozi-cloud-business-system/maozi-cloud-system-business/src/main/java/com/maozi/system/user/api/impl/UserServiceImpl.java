
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

import cn.hutool.extra.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.enums.Status;
import com.maozi.base.param.RequestParam;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.oauth.client.api.rpc.RpcClientService;
import com.maozi.oauth.token.api.rpc.RpcOauthTokenService;
import com.maozi.oauth.token.param.ClientUserParam;
import com.maozi.service.api.annotation.RemoteResource;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.permission.api.PermissionService;
import com.maozi.system.permission.api.RolePermissionService;
import com.maozi.system.permission.api.UserRoleService;
import com.maozi.system.redirect.enums.RedirectType;
import com.maozi.system.redirect.util.RedirectUtil;
import com.maozi.system.user.api.UserService;
import com.maozi.system.user.domain.UserDo;
import com.maozi.system.user.mapper.UserMapper;
import com.maozi.system.user.param.UserIndividualUpdateParam;
import com.maozi.system.user.param.UserSaveUpdateParam;
import com.maozi.system.user.result.OauthUserInfoResult;
import jakarta.annotation.Resource;
import lombok.Data;
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
@Data
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, UserDo> implements UserService {

    /** 资源名称常量，用于异常提示信息 */
    public static final String RESOURCE_NAME = "用户";

    /**
     * 密码编码器（DelegatingPasswordEncoder 委派模式，默认 {bcrypt} 算法且密文带算法前缀），
     * 用于用户密码的加密及旧密码比对校验
     */
    protected final static PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

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
    protected UserRoleService userRoleService;

    /** 权限服务，用于查询权限标识信息 */
    @Resource(name = "permissionServiceImpl")
    protected PermissionService permissionService;

    /** 客户端RPC服务，用于远程调用客户端相关接口 */
    @RemoteResource
    protected RpcClientService rpcClientService;

    /** OAuth令牌RPC服务，用于远程调用认证令牌相关接口 */
    @RemoteResource
    protected RpcOauthTokenService rpcOauthTokenService;

    /** 角色权限服务，用于查询角色关联的权限信息 */
    @Resource(name = "rolePermissionServiceImpl")
    protected RolePermissionService rolePermissionService;

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

        // 构建查询条件：按用户名查询，仅取 ID 字段
        MPJLambdaWrapper<UserDo> wrapper = MPJWrappers.lambdaJoin();

        wrapper.select(UserDo::getId);
        wrapper.eq(UserDo::getUsername,username);

        UserDo domain = getAvailableByParam(wrapper);

        return domain.getId();

    }

    /**
     * 用户新增或更新的统一处理方法
     * <p>
     * 当 id 为空时执行新增操作，当 id 不为空时执行更新操作。
     * 新增时（客户端ID与用户名均非空的前提下）会校验客户端是否可用
     * 以及用户名在对应客户端下是否重复。
     * 更新时不允许修改用户名和客户端ID。
     * 密码不为空时会进行加密处理。
     * 操作完成后同步更新用户的角色绑定关系；
     * 更新后状态为禁用时，销毁该用户的全部OAuth2令牌，强制下线。
     * </p>
     *
     * @param id    用户ID，新增时为空，更新时为已有用户ID
     * @param param 用户新增/更新参数
     * @return 新增或更新后的用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveUpdate(Long id, UserSaveUpdateParam param) {

        boolean hasId = ObjectUtil.isNotNullEmpty(id);
        if(hasId) {

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

        id = super.saveUpdate(id,param);

        // 保存后同步更新用户与角色的绑定关系
        userRoleService.updateBind(id, param.getBindRoleIds(), param.getUnbindRoleIds());

        if(hasId && Status.DISABLE == param.getStatus()){
            // 更新后状态为禁用时，销毁该用户的全部OAuth2令牌，强制下线
            UserDo user = getByIdThrowError(id, UserDo::getUsername, UserDo::getClientId);
            rpcOauthTokenService.rpcDestroyByPrincipal(user.getClientId(), user.getUsername()).getResultDataThrowError();
        }

        return id;

    }

    /**
     * 删除用户
     * <p>用户不存在时静默返回；删除时解除用户与角色的绑定关系，删除成功后销毁该用户的全部OAuth2令牌，强制下线。</p>
     *
     * @param id 需要删除的用户ID
     */
    public void remove(Long id) {

        // 先获取用户的用户名和客户端ID，删除后需用于注销认证令牌
        UserDo user = getById(id, UserDo::getUsername,UserDo::getClientId);
        if(ObjectUtil.isNullEmpty(user)){
            return;
        }

        // 删除用户数据库记录
        if(removeById(id)){
            // 通过RPC调用OAuth服务，根据clientId+用户名销毁该用户的所有OAuth2授权令牌
            rpcOauthTokenService.rpcDestroyByPrincipal(user.getClientId(), user.getUsername()).getResultDataThrowError();
        }

    }

    /**
     * 更新用户状态（启用/禁用）
     * <p>
     * 如果用户当前状态与目标状态一致，则不做任何操作直接返回；
     * 状态变更为禁用后，会远程销毁该用户的全部 OAuth2 令牌，强制其重新登录。
     * </p>
     *
     * @param id    需要更新状态的用户ID
     * @param param 状态参数，包含目标状态值
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, RequestParam<Status> param){

        Status status = param.getData();

        UserDo user = getByIdThrowError(id, UserDo::getUsername, UserDo::getClientId, UserDo::getStatus);

        // 当前状态与目标状态一致时，无需更新，直接返回
        if(user.getStatus() == status){
            return;
        }

        // 更新用户状态
        user.setId(id);
        user.setStatus(status);
        updateById(user);

        // 禁用用户后销毁其全部OAuth2令牌，强制下线
        if(Status.DISABLE == status){
            rpcOauthTokenService.rpcDestroyByPrincipal(user.getClientId(), user.getUsername()).getResultDataThrowError();
        }

    }

    /**
     * 更新当前登录用户的个人信息
     * <p>
     * 旧密码与新密码同时传入时先校验旧密码，校验通过后加密新密码一并更新；
     * 两者均未传入时不修改密码，仅更新其他基本信息。更新成功后销毁当前用户
     * 在该客户端下的全部 OAuth2 令牌，并通过 {@code X-Redirect} 响应头指示
     * 前端跳转登录页，强制重新登录。
     * </p>
     *
     * @param param 个人信息更新参数（姓名、头像、旧密码、新密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public void individualUpdate(UserIndividualUpdateParam param){

        // 当前登录用户 ID
        Long userId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUserId);

        // 旧密码与新密码
        String paramPassword = param.getPassword();
        String paramNewPassword = param.getNewPassword();

        // 旧密码与新密码同时传入时，先校验旧密码，再将新密码加密
        if(ObjectUtil.isNotNullEmpty(paramPassword) && ObjectUtil.isNotNullEmpty(paramNewPassword)){
            UserDo domain = getByIdThrowError(userId, UserDo::getPassword);

            // 旧密码与库中密文不匹配时抛出业务异常
            if(!UserServiceImpl.PASSWORD_ENCODER.matches(paramPassword,domain.getPassword())){
                throw new BusinessResultException("旧密码不正确");
            }

            paramNewPassword = UserServiceImpl.PASSWORD_ENCODER.encode(paramNewPassword);
        }

        // 复制参数并更新用户基本信息与新密码
        UserDo domain = CglibUtil.copy(param, UserDo.class);
        domain.setId(userId);
        domain.setPassword(paramNewPassword);
        updateById(domain);

        // 更新成功后销毁当前用户在该客户端下的全部OAuth2令牌，并指示前端跳转登录页，强制重新登录
        Long clientId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getClientId);
        String username = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUsername);
        rpcOauthTokenService.rpcDestroyByPrincipal(clientId, username).getResultDataThrowError();
        RedirectUtil.redirect(RedirectType.LOGIN);

    }

    /**
     * 根据用户名获取 OAuth 认证用户信息
     * <p>返回用户ID、加密密码及该用户的全部权限标识；用户名为空或用户不存在时抛出业务异常。</p>
     *
     * @param username 用户名
     * @return OAuth 认证用户信息（含 ID、密码、权限标识列表）
     */
    public OauthUserInfoResult getOauthUserInfoByUsername(String username){

        // 用户名为空时抛出数据不存在业务异常
        ObjectUtil.isNullEmptyThrowError(username, getResourceName());

        LambdaQueryWrapper<UserDo> wrapper = Wrappers.lambdaQuery();

        // 仅查询用户ID和密码字段
        wrapper.select(UserDo::getId,UserDo::getPassword);
        wrapper.eq(UserDo::getUsername,username);

        UserDo domain = getOne(wrapper);

        // 用户不存在时抛出业务异常
        ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

        // 查询权限标识并组装返回结果
        Long userId = domain.getId();
        List<String> permissions = getPermissionsByUserId(userId);

        return new OauthUserInfoResult(userId, domain.getPassword(), permissions);

    }

    /**
     * 根据用户名和指定角色ID获取该角色下的权限标识列表
     * <p>先校验用户可用且已绑定指定角色（未绑定时抛出异常），再查询该角色关联的权限标识。</p>
     *
     * @param username 用户名
     * @param roleId   角色ID
     * @return 指定角色下的权限标识列表
     */
    public List<String> getPermissionsByUsernameRole(String username, Long roleId) {

        // 校验并获取可用用户 ID
        Long id = getAvailableByUsername(username);

        // 校验用户绑定了指定角色，未绑定则抛出异常
        userRoleService.checkUserBindRole(id,roleId);

        List<String> responses = CollectionUtil.newArrayList();

        // 查询角色关联的权限ID集合，并转换为权限标识列表
        List<Long> permissionIds = rolePermissionService.getPermissionsByRole(roleId);
        List<String> marks = permissionService.getMarks(permissionIds);
        responses.addAll(marks);

        return responses;

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

        // 查询用户绑定的角色 ID 列表
        List<Long> roleIds = userRoleService.getRolesByUser(userId);

        if(!roleIds.isEmpty()) {

            // 通过角色查询关联的权限 ID 集合并转换为权限标识列表
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

        // 构建查询条件：按用户 ID 列表查询，仅取用户名与客户端 ID 字段
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
