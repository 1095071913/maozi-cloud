/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

import com.alibaba.csp.sentinel.slots.block.Rule;

import java.util.Date;

/**
 * Interface of entities that can be converted to a Sentinel {@link Rule}.
 *
 * @author leyou
 * @date 2026/11/22
 */
public interface RuleEntity {

    /** Gets the entity id. */
    Long getId();

    /** Sets the entity id. */
    void setId(Long id);

    /** Gets the application name. */
    String getApp();

    /** Gets the IP address of the machine. */
    String getIp();

    /** Gets the port of the machine. */
    Integer getPort();

    /** Gets the create time. */
    Date getGmtCreate();

    /** Sets the create time. */
    void setGmtCreate(Date gmtCreate);

    /** Sets the last modify time. */
    void setGmtModified(Date gmtModified);

    /** Sets both the create time and the last modify time to the given date. */
    default void setGmtCreateAndModified(Date date) {
        setGmtCreate(date);
        setGmtModified(date);
    }

    /** Converts this entity to a Sentinel rule. */
    Rule toRule();
}
