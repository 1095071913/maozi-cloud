package com.maozi.oauth.token.config.service;


import com.maozi.oauth.token.param.ClientUserParam;

import java.util.List;

/**
 * 扩展的OAuth2授权服务接口
 * <p>
 * 在标准{@link org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService}基础上
 * 扩展了按用户主体注销授权的能力，支持根据已注册客户端ID和用户主体名称移除授权记录，
 * 同时提供批量注销以减少Redis网络往返。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public interface OAuth2AuthorizationService extends org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService {

	/**
	 * 移除指定客户端下指定用户的所有授权记录（注销）
	 * <p>
	 * 通过用户主体索引查找该用户在该客户端下的所有授权ID，
	 * 并逐一移除，实现用户注销功能。
	 * </p>
	 *
	 * @param registeredClientId 已注册客户端ID
	 * @param principalName      用户主体名称（用户名）
	 */
	void removeAllByPrincipal(Long registeredClientId, String principalName);

	/**
	 * 批量移除多个用户的所有授权记录（批量注销）
	 * <p>
	 * 逐个用户读取其主体索引集合（N 次 SMEMBERS），再通过一次 MGET 一次性获取全部授权数据、
	 * 一次批量 DEL 删除所有关联键，总交互次数为 N+2 次（所有用户均无授权记录时为 N+1 次，
	 * 未使用 Pipeline），适用于角色禁用等需要同时注销多个用户的场景。
	 * </p>
	 *
	 * @param clientUsers 客户端用户参数列表，每项包含clientId和username
	 */
	void removeAllByPrincipals(List<ClientUserParam> clientUsers);

}

