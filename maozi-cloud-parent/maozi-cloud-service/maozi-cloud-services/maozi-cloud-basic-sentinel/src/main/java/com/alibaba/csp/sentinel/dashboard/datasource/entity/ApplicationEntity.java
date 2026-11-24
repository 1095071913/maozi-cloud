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

import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;

import java.util.Date;

/**
 * 注册到控制台的应用持久化实体。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class ApplicationEntity extends BaseEntity {

    /** 应用类型。 */
    private Integer appType;
    /** 应用当前生效的控制台地址。 */
    private String activeConsole;
    /** 最近一次拉取时间。 */
    private Date lastFetch;

    /** 获取应用类型。 */
    public Integer getAppType() {
        return appType;
    }

    /** 设置应用类型。 */
    public void setAppType(Integer appType) {
        this.appType = appType;
    }

    /** 获取应用当前生效的控制台地址。 */
    public String getActiveConsole() {
        return activeConsole;
    }

    /** 获取最近一次拉取时间。 */
    public Date getLastFetch() {
        return lastFetch;
    }

    /** 设置最近一次拉取时间。 */
    public void setLastFetch(Date lastFetch) {
        this.lastFetch = lastFetch;
    }

    /** 设置应用当前生效的控制台地址。 */
    public void setActiveConsole(String activeConsole) {
        this.activeConsole = activeConsole;
    }

    /** 转换为携带本实体应用名与应用类型的 {@link AppInfo}。 */
    public AppInfo toAppInfo() {
        return new AppInfo(app, appType);
    }

    /** 拼接除 appType 之外的字段构建字符串。 */
    @Override
    public String toString() {
        return "ApplicationEntity{"
                + "id=" + id
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + ", app='" + app + '\''
                + ", activeConsole='" + activeConsole + '\''
                + ", lastFetch=" + lastFetch
                + '}';
    }
}
