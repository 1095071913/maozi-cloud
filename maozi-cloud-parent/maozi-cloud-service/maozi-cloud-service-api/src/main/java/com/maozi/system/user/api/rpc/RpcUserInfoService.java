package com.maozi.system.user.api.rpc;

import com.maozi.common.result.AbstractBaseResult;
import com.maozi.system.user.dto.SystemUser;

/**
 * 系统用户 RPC 服务接口
 * <p>
 * 定义系统用户信息相关的 RPC 远程调用方法。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public interface RpcUserInfoService {

	/**
	 * 根据用户 ID 查询系统用户信息
	 * <p>
	 * 查询用户及其关联数据并转换为 {@link SystemUser}，用户不存在时抛出业务异常。
	 * </p>
	 *
	 * @param id 用户 ID
	 * @param columns 查询列（保留参数，当前实现未使用，返回完整用户信息）
	 * @return 系统用户信息封装的统一结果
	 * @throws com.maozi.common.result.error.exception.BusinessResultException 用户不存在时抛出
	 */
	AbstractBaseResult<SystemUser> rpcGet(Long id, String ... columns);

}
