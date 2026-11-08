package com.maozi.common.constant;

/**
 * 远程调用常量类
 * <p>
 * 定义服务间远程调用（Feign）接口的路径前缀，内部远程接口统一携带该前缀，
 * 便于与对外 API 区分，并支持在 Swagger 文档等场景下按前缀统一排除。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/7/9 10:12
 */
public final class RemoteConstant {

    /** 私有构造器，防止常量类被实例化 */
    private RemoteConstant() {}

    /** 远程调用接口统一前缀路径 */
    public static final String REMOTE_PREFIX_PATH = "/remote";

}
