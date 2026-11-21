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
package com.alibaba.csp.sentinel.dashboard.datasource.entity;

import java.util.Date;

/**
 * 控制台实体的公共基类，持有公共字段：id、app、gmtCreate 与 gmtModified。
 *
 * @author leyou
 * @date 2026/11/22
 */
public abstract class BaseEntity {

    /** 实体 id。 */
    protected Long id;
    /** 应用名。 */
    protected String app;
    /** 创建时间。 */
    protected Date gmtCreate;
    /** 最近修改时间。 */
    protected Date gmtModified;

    /** 获取实体 id。 */
    public Long getId() {
        return id;
    }

    /** 设置实体 id。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取应用名。 */
    public String getApp() {
        return app;
    }

    /** 设置应用名并返回当前对象。 */
    public BaseEntity setApp(String app) {
        this.app = app;
        return this;
    }

    /** 获取创建时间。 */
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /** 设置创建时间。 */
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /** 获取最近修改时间。 */
    public Date getGmtModified() {
        return gmtModified;
    }

    /** 设置最近修改时间。 */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
}
