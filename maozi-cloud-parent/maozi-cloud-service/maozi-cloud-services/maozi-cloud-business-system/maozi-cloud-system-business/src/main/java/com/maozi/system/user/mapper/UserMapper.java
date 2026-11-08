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

package com.maozi.system.user.mapper;

import com.maozi.service.api.IBaseMapper;
import com.maozi.system.user.domain.UserDo;

/**
 * 用户Mapper接口
 * <p>
 * 对应表 system_user，提供用户记录的增删改查等数据库操作。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public interface UserMapper extends IBaseMapper<UserDo>{}