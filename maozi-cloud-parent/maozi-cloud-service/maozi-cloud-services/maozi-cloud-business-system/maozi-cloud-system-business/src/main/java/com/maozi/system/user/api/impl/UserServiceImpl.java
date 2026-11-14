
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
import com.maozi.system.user.domain.UserDO;
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
 * 提供用户的增删改查、状态管理、个人信息维护及权限查询等业务逻辑，
 * 同时作为 REST 与 RPC 服务实现的公共基础。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, UserDO> implements UserService {

    /** 资源名称常量，用于异常提示信息 */
    public static final String RESOURCE_NAME = "用户";

    /** 密码编码器（DelegatingPasswordEncoder，默认 {bcrypt} 算法），用于密码加密与比对校验 */
    protected final static PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    /** 获取资源名称 */
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
     * 解绑用户的所有角色关联关系
     *
     * @param id 用户ID
     */
    @Override
    public void unbind(Long id) {
        userRoleService.userUnbind(id);
    }

    /**
     * 根据用户ID获取客户端用户信息
     * <p>用户不存在时抛出业务异常。</p>
     *
     * @param id 用户ID
     * @return 客户端用户信息（用户名与客户端ID）
     */
    @Override
    public ClientUserParam getClientUser(Long id) {
        return super.getByIdThrowError(id, ClientUserParam.class);
    }

    /**
     * 根据用户ID列表批量获取客户端用户信息
     * <p>ID列表为空时抛出业务异常。</p>
     *
     * @param ids 用户ID列表
     * @return 客户端用户信息列表
     */
    @Override
    public List<ClientUserParam> getClientUsers(List<Long> ids) {

        CollectionUtil.collectionIsEmptyThrowError(ids, getResourceName() + "列表");

        // 构建查询条件：按用户 ID 列表查询，仅取用户名与客户端 ID 字段
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.lambdaQuery();

        wrapper.select(UserDO::getUsername,UserDO::getClientId);

        wrapper.in(UserDO::getId, ids);

        return super.list(wrapper, ClientUserParam::new);

    }

    /**
     * 用户新增或更新的统一处理方法
     * <p>
     * id 为空时新增（校验客户端可用性及用户名唯一性），否则更新（不允许修改用户名与客户端ID）；
     * 密码非空时加密入库，并同步维护角色绑定关系，更新后状态为禁用时销毁该用户全部 OAuth2 令牌强制下线。
     * </p>
     *
     * @param id    用户ID，新增时为空，更新时为已有用户ID
     * @param param 用户新增/更新参数
     * @return 新增或更新后的用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long adminSaveUpdate(Long id, UserSaveUpdateParam param) {

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
                super.checkNotHas(MPJWrappers.lambdaJoin(UserDO.builder()
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
            UserDO user = super.getByIdThrowError(id, UserDO::getUsername, UserDO::getClientId);
            rpcOauthTokenService.destroyByPrincipalResult(user.getClientId(), user.getUsername()).getResultDataThrowError();
        }

        return id;

    }

    /**
     * 删除用户
     * <p>用户不存在时静默返回；删除时解除用户与角色的绑定关系，删除成功后销毁该用户的全部OAuth2令牌，强制下线。</p>
     *
     * @param id 需要删除的用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminRemove(Long id) {

        // 先获取用户的用户名和客户端ID，删除后需用于注销认证令牌
        UserDO user = super.getById(id, UserDO::getUsername,UserDO::getClientId);
        if(ObjectUtil.isNullEmpty(user)){
            return;
        }

        if(super.removeById(id)){
            // 删除成功后销毁该用户全部 OAuth2 令牌，强制下线
            rpcOauthTokenService.destroyByPrincipalResult(user.getClientId(), user.getUsername()).getResultDataThrowError();
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
    public void adminUpdateStatus(Long id, RequestParam<Status> param){

        Status status = param.getData();

        UserDO user = super.getByIdThrowError(id, UserDO::getUsername, UserDO::getClientId, UserDO::getStatus);

        // 当前状态与目标状态一致时，无需更新，直接返回
        if(user.getStatus() == status){
            return;
        }

        // 更新用户状态
        user.setId(id);
        user.setStatus(status);
        super.updateById(user);

        // 禁用用户后销毁其全部OAuth2令牌，强制下线
        if(Status.DISABLE == status){
            rpcOauthTokenService.destroyByPrincipalResult(user.getClientId(), user.getUsername()).getResultDataThrowError();
        }

    }

    /**
     * 更新当前登录用户的个人信息
     * <p>
     * 旧密码与新密码需同时传入，校验旧密码通过后将新密码加密一并更新，两者均未传入时不修改密码；
     * 更新成功后销毁当前用户的全部 OAuth2 令牌，并通过 {@code X-Redirect} 响应头指示前端跳转登录页，强制重新登录。
     * </p>
     *
     * @param param 个人信息更新参数（姓名、头像、旧密码、新密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminIndividualUpdate(UserIndividualUpdateParam param){

        Long userId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUserId);

        String paramPassword = param.getPassword();
        String paramNewPassword = param.getNewPassword();

        // 旧密码与新密码同时传入时，先校验旧密码，再将新密码加密
        if(ObjectUtil.isNotNullEmpty(paramPassword) && ObjectUtil.isNotNullEmpty(paramNewPassword)){
            UserDO domain = super.getByIdThrowError(userId, UserDO::getPassword);

            // 旧密码与库中密文不匹配时抛出业务异常
            if(!UserServiceImpl.PASSWORD_ENCODER.matches(paramPassword,domain.getPassword())){
                throw new BusinessResultException("旧密码不正确");
            }

            paramNewPassword = UserServiceImpl.PASSWORD_ENCODER.encode(paramNewPassword);
        }

        // 复制参数并更新用户基本信息与新密码
        UserDO domain = CglibUtil.copy(param, UserDO.class);
        domain.setId(userId);
        domain.setPassword(paramNewPassword);
        super.updateById(domain);

        // 更新成功后销毁当前用户在该客户端下的全部OAuth2令牌，并指示前端跳转登录页，强制重新登录
        Long clientId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getClientId);
        String username = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUsername);
        rpcOauthTokenService.destroyByPrincipalResult(clientId, username).getResultDataThrowError();
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

        LambdaQueryWrapper<UserDO> wrapper = Wrappers.lambdaQuery();

        // 仅查询用户ID和密码字段
        wrapper.select(UserDO::getId,UserDO::getPassword);
        wrapper.eq(UserDO::getUsername,username);

        UserDO domain = super.getOne(wrapper);

        // 用户不存在时抛出业务异常
        ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

        // 查询权限标识并组装返回结果
        Long userId = domain.getId();
        List<String> permissions = getPermissionsByUserId(userId);

        return new OauthUserInfoResult(userId, domain.getPassword(), permissions);

    }

    /**
     * 获取当前登录用户的权限列表
     *
     * @return 当前用户拥有的权限标识列表，无认证信息时返回空列表
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
     * 根据用户ID获取该用户的权限标识列表
     * <p>先查用户绑定的角色，再查角色关联的权限并转换为权限标识；未绑定角色时返回空列表。</p>
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

}
