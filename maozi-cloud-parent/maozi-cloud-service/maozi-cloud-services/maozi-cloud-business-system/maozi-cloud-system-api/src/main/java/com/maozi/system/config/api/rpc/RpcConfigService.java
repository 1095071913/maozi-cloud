package com.maozi.system.config.api.rpc;

import com.maozi.common.result.AbstractBaseResult;

/**
 * 全局配置 RPC 服务接口
 * <p>
 * 定义全局配置相关的 RPC 远程调用方法，
 * 供其他微服务通过 RPC 方式获取系统全局配置。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface RpcConfigService {

	/**
	 * 根据配置键名获取配置值
	 * <p>
	 * 配置键名即配置名称（name），全局唯一。
	 * 配置键名为空时抛出业务异常；配置不存在时返回成功响应且配置值为空（null）。
	 * </p>
	 *
	 * @param key 配置键名（对应配置名称），不能为空
	 * @return 返回包含配置值的统一响应结果，配置不存在时配置值为空（null）
	 */
	AbstractBaseResult<String> getResult(String key);

}
