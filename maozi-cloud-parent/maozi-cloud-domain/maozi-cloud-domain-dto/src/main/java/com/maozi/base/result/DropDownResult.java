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

package com.maozi.base.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 下拉选项结果
 * <p>
 * 通用的下拉选择框数据结构，包含 ID 和名称。
 * 适用于所有需要下拉选项的场景。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropDownResult implements Serializable {

	/** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 选项 ID */
	@Schema(description = "ID")
	private Long id;

	/** 选项名称 */
	@Schema(description = "名称")
	private String name;

}
