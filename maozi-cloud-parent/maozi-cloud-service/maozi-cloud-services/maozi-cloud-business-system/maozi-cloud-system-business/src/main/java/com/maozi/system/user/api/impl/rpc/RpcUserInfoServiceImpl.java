package com.maozi.system.user.api.impl.rpc;

import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.user.api.impl.UserServiceImpl;
import com.maozi.system.user.api.rpc.RpcUserInfoService;
import com.maozi.system.user.dto.SystemUser;

/**
 * 用户信息RPC服务实现类
 * <p>
 * 提供用户信息的 Dubbo RPC 远程调用接口实现，内部持有 UserServiceImpl 复用用户基础业务逻辑。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcUserInfoServiceImpl extends BaseServiceResultImpl<UserServiceImpl, Void> implements RpcUserInfoService {

	/**
	 * 根据用户 ID 查询系统用户信息
	 * <p>用户不存在时抛出业务异常。</p>
	 *
	 * @param id 用户 ID
	 * @param columns 查询列（保留参数，当前未使用）
	 * @return 系统用户信息封装的统一结果
	 * @throws com.maozi.common.result.error.exception.BusinessResultException 用户不存在时抛出
	 */
	@Override
	public AbstractBaseResult<SystemUser> getResult(Long id, String... columns) {
		return ResultUtil.success(service.getByIdThrowErrorRelation(id, SystemUser.class));
	}

}
