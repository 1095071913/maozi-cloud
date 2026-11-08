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
 */

package com.maozi.db.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.maozi.base.enums.Deleted;
import com.maozi.base.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体公共基础字段抽象父类
 * <p>
 * 封装主键、逻辑删除标志、业务状态、创建时间等通用字段，子类继承即可复用；
 * deleted、status、createTime 在插入时由 MetaObjectHandler 自动填充默认值。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractBaseDomain implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 主键 ID，由数据库自增策略生成 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/** 逻辑删除标志，删除操作转为更新此字段，插入时自动填充默认值 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private Deleted deleted;

	/** 业务状态（启用/禁用），插入时自动填充默认值 */
	@TableField(fill = FieldFill.INSERT)
	private Status status;

	/** 创建时间，插入时自动填充为当前时间 */
	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;

}
