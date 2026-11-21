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
 * 可转换为 Sentinel {@link Rule} 的规则实体接口。
 *
 * @author leyou
 * @date 2026/11/22
 */
public interface RuleEntity {

    /**
     * 获取实体 id。
     *
     * @return 实体 id
     */
    Long getId();

    /**
     * 设置实体 id。
     *
     * @param id 实体 id
     */
    void setId(Long id);

    /**
     * 获取应用名。
     *
     * @return 应用名
     */
    String getApp();

    /**
     * 获取机器 IP 地址。
     *
     * @return 机器 IP 地址
     */
    String getIp();

    /**
     * 获取机器端口。
     *
     * @return 机器端口
     */
    Integer getPort();

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    Date getGmtCreate();

    /**
     * 设置创建时间。
     *
     * @param gmtCreate 创建时间
     */
    void setGmtCreate(Date gmtCreate);

    /**
     * 设置最后修改时间。
     *
     * @param gmtModified 最后修改时间
     */
    void setGmtModified(Date gmtModified);

    /**
     * 同时设置创建时间与最后修改时间为指定时间。
     *
     * @param date 创建时间与最后修改时间
     */
    default void setGmtCreateAndModified(Date date) {
        setGmtCreate(date);
        setGmtModified(date);
    }

    /**
     * 将实体转换为 Sentinel 规则。
     *
     * @return Sentinel 规则
     */
    Rule toRule();
}
