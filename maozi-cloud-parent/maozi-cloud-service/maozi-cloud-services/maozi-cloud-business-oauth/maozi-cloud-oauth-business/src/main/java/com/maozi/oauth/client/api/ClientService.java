package com.maozi.oauth.client.api;

import java.util.Collection;
import java.util.Map;

/**
 * OAuth2客户端服务接口。
 * <p>
 * 定义客户端信息的查询方法，包括根据主键获取客户端ID
 * 以及批量获取客户端ID映射关系，由 ClientServiceImpl 提供实现，
 * 并注入到 RPC/REST 服务实现中使用。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface ClientService {

	/**
	 * 根据客户端主键ID获取客户端标识（clientId）。
	 *
	 * @param id 客户端主键ID
	 * @return 客户端标识字符串
	 */
	String getClientId(Long id);

	/**
	 * 根据客户端主键ID集合批量获取客户端ID与clientId的映射关系。
	 *
	 * @param ids 客户端主键ID集合
	 * @return 键为主键ID、值为clientId的映射Map
	 */
	Map<Long,String> getClientIdsMap(Collection<Long> ids);

}