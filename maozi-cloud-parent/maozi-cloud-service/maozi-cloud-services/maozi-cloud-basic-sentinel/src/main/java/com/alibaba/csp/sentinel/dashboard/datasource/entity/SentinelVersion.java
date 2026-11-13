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
 * Sentinel version composed of major, minor and fix version numbers plus an optional postfix.
 *
 * @author Eric Zhao
 * @since 0.2.1
 */
public class SentinelVersion {
    /** The major version number. */
    private int majorVersion;
    /** The minor version number. */
    private int minorVersion;
    /** The fix version number. */
    private int fixVersion;
    /** The postfix of the version, may be null. */
    private String postfix;
    
    /** Create a version of 0.0.0. */
    public SentinelVersion() {
        this(0, 0, 0);
    }
    
    /** Create a version of the given numbers without postfix. */
    public SentinelVersion(int major, int minor, int fix) {
        this(major, minor, fix, null);
    }
    
    /** Create a version of the given numbers and postfix. */
    public SentinelVersion(int major, int minor, int fix, String postfix) {
        this.majorVersion = major;
        this.minorVersion = minor;
        this.fixVersion = fix;
        this.postfix = postfix;
    }
    
    /** Get the full version as a single integer in the form major * 1000000 + minor * 1000 + fix. */
    public int getFullVersion() {
        return majorVersion * 1000000 + minorVersion * 1000 + fixVersion;
    }

    /** Get the major version number. */
    public int getMajorVersion() {
        return majorVersion;
    }

    /** Set the major version number and return this. */
    public SentinelVersion setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
        return this;
    }

    /** Get the minor version number. */
    public int getMinorVersion() {
        return minorVersion;
    }

    /** Set the minor version number and return this. */
    public SentinelVersion setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
        return this;
    }

    /** Get the fix version number. */
    public int getFixVersion() {
        return fixVersion;
    }

    /** Set the fix version number and return this. */
    public SentinelVersion setFixVersion(int fixVersion) {
        this.fixVersion = fixVersion;
        return this;
    }

    /** Get the postfix of the version. */
    public String getPostfix() {
        return postfix;
    }

    /** Set the postfix of the version and return this. */
    public SentinelVersion setPostfix(String postfix) {
        this.postfix = postfix;
        return this;
    }

    /**
     * Check whether this version is greater than the given one; a null version is treated
     * as the smallest one.
     */
    public boolean greaterThan(SentinelVersion version) {
        if (version == null) {
            return true;
        }
        return getFullVersion() > version.getFullVersion();
    }

    /**
     * Check whether this version is greater than or equal to the given one; a null version
     * is treated as the smallest one.
     */
    public boolean greaterOrEqual(SentinelVersion version) {
        if (version == null) {
            return true;
        }
        return getFullVersion() >= version.getFullVersion();
    }

    /** Whether the given object is a version with the same numbers and postfix. */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        SentinelVersion that = (SentinelVersion)o;

        if (getFullVersion() != that.getFullVersion()) { return false; }
        return postfix != null ? postfix.equals(that.postfix) : that.postfix == null;
    }

    /** Get the hash code from all version numbers and the postfix. */
    @Override
    public int hashCode() {
        int result = majorVersion;
        result = 31 * result + minorVersion;
        result = 31 * result + fixVersion;
        result = 31 * result + (postfix != null ? postfix.hashCode() : 0);
        return result;
    }

    /** Build a string with all fields of this version. */
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
