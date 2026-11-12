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

package com.maozi.oauth.client.mapper;

import com.maozi.oauth.client.domain.ClientDo;
import com.maozi.service.api.IBaseMapper;


/**
 * OAuth2客户端Mapper接口。
 * <p>
 * 继承 IBaseMapper<ClientDo>，提供客户端实体与数据库表 oauth2_registered_client
 * 之间的ORM映射操作，包括基础的增删改查和关联查询功能。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface ClientMapper extends IBaseMapper<ClientDo>{}
