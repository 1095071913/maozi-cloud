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
 * Base entity holding the common fields shared by dashboard entities: id, app,
 * gmtCreate and gmtModified.
 *
 * @author leyou
 */
public abstract class BaseEntity {

    /** The entity id. */
    protected Long id;
    /** The application name. */
    protected String app;
    /** The create time. */
    protected Date gmtCreate;
    /** The last modify time. */
    protected Date gmtModified;

    /** Gets the entity id. */
    public Long getId() {
        return id;
    }

    /** Sets the entity id. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Gets the application name. */
    public String getApp() {
        return app;
    }

    /** Sets the application name and returns this. */
    public BaseEntity setApp(String app) {
        this.app = app;
        return this;
    }

    /** Gets the create time. */
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /** Sets the create time. */
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /** Gets the last modify time. */
    public Date getGmtModified() {
        return gmtModified;
    }

    /** Sets the last modify time. */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
}
