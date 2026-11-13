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
 * 提供客户端（OAuth2 Client）的远程过程调用（RPC）接口定义，
 * 用于微服务之间的内部调用，包括新增客户端、客户端可用性校验
 * 以及单个和批量获取客户端下拉信息等功能。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface RpcClientService {

	/**
	 * 新增客户端并返回保存后的客户端信息
	 * <p>
	 * 供AI智能体等远程调用方使用：先以新增方式保存客户端，
	 * 再根据保存后的主键ID查询并返回客户端详细信息DTO。
	 * </p>
	 *
	 * @param param 客户端保存参数
	 * @return 返回包含新增客户端信息的DTO结果
	 */
	AbstractBaseResult<ClientDTO> rpcAiSave(ClientSaveUpdateParam param);

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
	AbstractBaseResult<DropDownResult> dropDownResult(Long id);

	/**
	 * 根据客户端 ID 集合批量获取客户端的下拉选项信息列表
	 *
	 * @param ids 客户端唯一标识 ID 集合
	 * @return 返回客户端下拉选项结果列表
	 */
	AbstractBaseResult<List<DropDownResult>> dropDownListResult(Collection<Long> ids);

}