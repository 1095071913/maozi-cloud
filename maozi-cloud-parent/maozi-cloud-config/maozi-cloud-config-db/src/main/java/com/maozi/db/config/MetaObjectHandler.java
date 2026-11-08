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

package com.maozi.db.config;

import com.maozi.base.enums.Deleted;
import com.maozi.base.enums.Status;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * <p>
 * 在执行插入操作时自动填充状态（默认启用）、逻辑删除（默认未删除）和创建时间字段。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class MetaObjectHandler implements com.baomidou.mybatisplus.core.handlers.MetaObjectHandler {

    /** 状态字段名 */
    private static final String STATUS = "status";

    /** 逻辑删除字段名 */
    private static final String DELETED = "deleted";

    /** 创建时间字段名 */
    private static final String CREATE_TIME = "createTime";

    /**
     * 插入时自动填充字段
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 自动填充状态字段，默认值为启用（ENABLE）
        this.strictInsertFill(metaObject, STATUS, Status.class, Status.ENABLE);
        // 自动填充逻辑删除字段，默认值为未删除（NONE）
        this.strictInsertFill(metaObject, DELETED, Deleted.class, Deleted.NONE);
        // 自动填充创建时间字段，默认值为当前时间
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充字段（暂无实现）
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {}
}
