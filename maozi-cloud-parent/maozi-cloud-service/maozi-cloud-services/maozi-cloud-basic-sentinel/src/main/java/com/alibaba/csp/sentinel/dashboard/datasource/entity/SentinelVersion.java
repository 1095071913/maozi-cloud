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

/**
 * 由主版本号、次版本号、修复版本号及可选后缀组成的 Sentinel 版本。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
public class SentinelVersion {
    /** 主版本号。 */
    private int majorVersion;
    /** 次版本号。 */
    private int minorVersion;
    /** 修复版本号。 */
    private int fixVersion;
    /** 版本后缀，可为 null。 */
    private String postfix;
    
    /** 构造 0.0.0 版本。 */
    public SentinelVersion() {
        this(0, 0, 0);
    }
    
    /** 以指定版本号构造无后缀版本。 */
    public SentinelVersion(int major, int minor, int fix) {
        this(major, minor, fix, null);
    }
    
    /** 以指定版本号与后缀构造版本。 */
    public SentinelVersion(int major, int minor, int fix, String postfix) {
        this.majorVersion = major;
        this.minorVersion = minor;
        this.fixVersion = fix;
        this.postfix = postfix;
    }
    
    /** 获取整数形式的完整版本号：major * 1000000 + minor * 1000 + fix。 */
    public int getFullVersion() {
        return majorVersion * 1000000 + minorVersion * 1000 + fixVersion;
    }

    /** 获取主版本号。 */
    public int getMajorVersion() {
        return majorVersion;
    }

    /** 设置主版本号并返回当前对象。 */
    public SentinelVersion setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
        return this;
    }

    /** 获取次版本号。 */
    public int getMinorVersion() {
        return minorVersion;
    }

    /** 设置次版本号并返回当前对象。 */
    public SentinelVersion setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
        return this;
    }

    /** 获取修复版本号。 */
    public int getFixVersion() {
        return fixVersion;
    }

    /** 设置修复版本号并返回当前对象。 */
    public SentinelVersion setFixVersion(int fixVersion) {
        this.fixVersion = fixVersion;
        return this;
    }

    /** 获取版本后缀。 */
    public String getPostfix() {
        return postfix;
    }

    /** 设置版本后缀并返回当前对象。 */
    public SentinelVersion setPostfix(String postfix) {
        this.postfix = postfix;
        return this;
    }

    /**
     * 判断当前版本是否大于指定版本；null 版本视为最小版本。
     */
    public boolean greaterThan(SentinelVersion version) {
        if (version == null) {
            return true;
        }
        return getFullVersion() > version.getFullVersion();
    }

    /**
     * 判断当前版本是否大于等于指定版本；null 版本视为最小版本。
     */
    public boolean greaterOrEqual(SentinelVersion version) {
        if (version == null) {
            return true;
        }
        return getFullVersion() >= version.getFullVersion();
    }

    /** 判断指定对象是否为版本号与后缀均相同的版本。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        SentinelVersion that = (SentinelVersion)o;

        if (getFullVersion() != that.getFullVersion()) { return false; }
        return postfix != null ? postfix.equals(that.postfix) : that.postfix == null;
    }

    /** 以全部版本号与后缀计算哈希码。 */
    @Override
    public int hashCode() {
        int result = majorVersion;
        result = 31 * result + minorVersion;
        result = 31 * result + fixVersion;
        result = 31 * result + (postfix != null ? postfix.hashCode() : 0);
        return result;
    }

    /** 拼接全部字段构建字符串。 */
    @Override
    public String toString() {
        return "SentinelVersion{" +
            "majorVersion=" + majorVersion +
            ", minorVersion=" + minorVersion +
            ", fixVersion=" + fixVersion +
            ", postfix='" + postfix + '\'' +
            '}';
    }
}
