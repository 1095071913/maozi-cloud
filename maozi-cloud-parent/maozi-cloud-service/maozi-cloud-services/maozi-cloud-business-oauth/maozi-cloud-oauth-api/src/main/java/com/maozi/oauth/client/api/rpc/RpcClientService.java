package com.maozi.oauth.client.api.rpc;

import com.maozi.base.result.DropDownResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.client.dto.ClientDTO;
import com.maozi.oauth.client.param.ClientSaveUpdateParam;

import java.util.Collection;
import java.util.List;

/**
 * 客户端管理 RPC 接口
 * <p>
 * 提供客户端（OAuth2 Client）的RPC接口定义，供微服务之间内部调用，
 * 包括新增、可用性校验及下拉信息查询等功能。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface RpcClientService {

	/**
	 * 新增客户端并按保存后的主键ID查询返回客户端DTO
	 *
	 * @param param 客户端保存参数
	 * @return 返回包含新增客户端信息的DTO结果
	 */
	AbstractBaseResult<ClientDTO> aiSaveResult(ClientSaveUpdateParam param);

	/**
	 * 校验指定客户端是否可用（存在且未禁用）
	 *
	 * @param id 客户端唯一标识 ID
	 * @return 空结果
	 * @throws com.maozi.common.result.error.exception.BusinessResultException 客户端不存在或已禁用时抛出
	 */
	AbstractBaseResult<Void> checkAvailableResult(Long id);

	/**
	 * 根据客户端 ID 获取单个客户端的下拉选项信息
	 *
	 * @param id 客户端唯一标识 ID
	 * @return 返回客户端的下拉选项结果
	 */
	AbstractBaseResult<? extends DropDownResult> dropDownResult(Long id);

	/**
	 * 根据客户端 ID 集合批量获取客户端的下拉选项信息列表
	 *
	 * @param ids 客户端唯一标识 ID 集合
	 * @return 返回客户端下拉选项结果列表
	 */
	AbstractBaseResult<List<? extends DropDownResult>> dropDownListResult(Collection<Long> ids);

}