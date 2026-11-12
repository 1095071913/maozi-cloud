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

package com.maozi.system.permission.api;

import java.util.Collection;
import java.util.List;

/**
 * 权限服务接口
 * <p>提供权限相关的业务操作，包括权限存在性判断和权限标识查询等功能。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface PermissionService {

	/**
	 * 判断指定权限是否存在
	 *
	 * @param id 权限ID
	 * @return 如果权限存在返回true，否则返回false
	 */
	boolean has(Long id);

	/**
	 * 根据权限ID集合获取权限标识列表
	 *
	 * @param ids 权限ID集合
	 * @return 权限标识（mark）列表
	 */
	List<String> getMarks(Collection<Long> ids);

}
