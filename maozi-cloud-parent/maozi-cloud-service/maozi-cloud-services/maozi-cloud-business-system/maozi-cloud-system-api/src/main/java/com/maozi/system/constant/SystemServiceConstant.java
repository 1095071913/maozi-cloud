package com.maozi.system.constant;

/**
 * 系统模块服务 Bean 名称常量类
 * <p>
 * 集中定义 system 模块内各服务实现类的 Spring Bean 名称，
 * 供 {@code @Resource(name = ...)} 注入与 {@code @QueryMapping(serviceName = ...)}
 * 自动查询填充统一引用，避免魔法字符串散落多处、实现类改名时遗漏。
 * </p>
 *
 * @author maozi
 * @date 2026/11/16
 */
public final class SystemServiceConstant {

    /** 用户角色关系服务实现类 Bean 名称 */
    public static final String USER_ROLE_SERVICE = "userRoleServiceImpl";

    /** 角色权限关系服务实现类 Bean 名称 */
    public static final String ROLE_PERMISSION_SERVICE = "rolePermissionServiceImpl";

    /** 权限服务实现类 Bean 名称 */
    public static final String PERMISSION_SERVICE = "permissionServiceImpl";

    /** 用户服务实现类 Bean 名称 */
    public static final String USER_SERVICE = "userServiceImpl";

    /** 角色服务实现类 Bean 名称 */
    public static final String ROLE_SERVICE = "roleServiceImpl";

    /** OAuth 客户端 RPC 服务 Bean 名称（oauth 模块提供） */
    public static final String RPC_CLIENT_SERVICE = "rpcClientService";

    /** 工具类屏蔽公共构造器 */
    private SystemServiceConstant() {}

}
