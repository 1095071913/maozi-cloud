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
package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nacos 规则持久化工具类
 * <p>
 * 各规则类型的 DataId 为 {@code appName + DATA_ID_POSTFIX} 加 {@code .json}
 * 后缀，如 {@code foo-app-flow-rules.json}。配置内容为格式化的 JSON，
 * 并以 {@code json} 配置类型发布
 *
 * @author maozi
 * @date 2026/11/08
 */
public final class NacosConfigUtil {

    /** 全部 Sentinel 规则配置的默认 Nacos 分组 */
    public static final String GROUP_ID = "SENTINEL_GROUP";

    /** 从 Nacos 单次读取最新配置的超时时间（毫秒），所有规则提供者共用 */
    public static final long READ_TIMEOUT_MS = 3000L;

    /** 流控规则的 DataId 后缀 */
    public static final String FLOW_DATA_ID_POSTFIX = "-flow-rules.json";
    /** 熔断规则的 DataId 后缀 */
    public static final String DEGRADE_DATA_ID_POSTFIX = "-degrade-rules.json";
    /** 热点参数流控规则的 DataId 后缀 */
    public static final String PARAM_FLOW_DATA_ID_POSTFIX = "-param-rules.json";
    /** 系统规则的 DataId 后缀 */
    public static final String SYSTEM_DATA_ID_POSTFIX = "-system-rules.json";
    /** 授权规则的 DataId 后缀 */
    public static final String AUTHORITY_DATA_ID_POSTFIX = "-authority-rules.json";
    /** 网关流控规则的 DataId 后缀 */
    public static final String GATEWAY_FLOW_DATA_ID_POSTFIX = "-gw-flow-rules.json";
    /** 网关 API 定义的 DataId 后缀 */
    public static final String GATEWAY_API_DATA_ID_POSTFIX = "-gw-api-rules.json";
    /** 集群分配映射的 DataId 后缀（无 {@code .json} 后缀） */
    public static final String CLUSTER_MAP_DATA_ID_POSTFIX = "-cluster-map";

    /** 集群客户端配置的 DataId 后缀（{@code cc} 即 cluster-client） */
    public static final String CLIENT_CONFIG_DATA_ID_POSTFIX = "-cc-config";
    /** 集群服务端传输配置的 DataId 后缀（{@code cs} 即 cluster-server） */
    public static final String SERVER_TRANSPORT_CONFIG_DATA_ID_POSTFIX = "-cs-transport-config";
    /** 集群服务端流控配置的 DataId 后缀 */
    public static final String SERVER_FLOW_CONFIG_DATA_ID_POSTFIX = "-cs-flow-config";
    /** 集群服务端命名空间集合的 DataId 后缀 */
    public static final String SERVER_NAMESPACE_SET_DATA_ID_POSTFIX = "-cs-namespace-set";

    /** 发布快照缓存的初始容量 */
    private static final int SNAPSHOT_CACHE_INITIAL_CAPACITY = 16;

    /**
     * 发布快照的存活时间：{@link ConfigService#getConfig} 读取的是本地客户端
     * 缓存，该缓存依赖服务端推送刷新，发布后立即读取可能返回旧的缓存内容
     * （前端保存后会立刻刷新规则列表，曾导致修改"看似未生效"需手动刷新）。
     * 发布成功后的这段短时间内改读刚发布内容的快照
     */
    private static final long SNAPSHOT_TTL_MS = 5000;

    /**
     * 最近发布的配置内容，key 为 {@code dataId@group}，
     * 由 {@link #readFreshConfig} 在发布后 {@link #SNAPSHOT_TTL_MS} 内使用
     */
    private static final Map<String, PublishSnapshot> PUBLISH_SNAPSHOTS =
            new ConcurrentHashMap<>(SNAPSHOT_CACHE_INITIAL_CAPACITY);

    /**
     * 刚发布的配置内容的不可变记录
     *
     * @author maozi
     * @date 2026/11/08
     */
    private static final class PublishSnapshot {
        /** 发布的配置内容 */
        private final String content;
        /** 发布时刻的毫秒时间戳 */
        private final long publishTime;

        /** 记录发布内容与发布时间戳 */
        private PublishSnapshot(String content, long publishTime) {
            this.content = content;
            this.publishTime = publishTime;
        }
    }

    /**
     * 将规则以格式化 JSON 发布到 {@code appName + dataIdPostfix}，
     * Nacos 配置类型标记为 {@code json}
     */
    public static void publishJsonConfig(ConfigService configService, String appName,
            String dataIdPostfix, String group, Object rules) throws NacosException {
        AssertUtil.notEmpty(appName, "app name cannot be empty");
        String dataId = appName + dataIdPostfix;
        String content = JSON.toJSONString(rules, SerializerFeature.PrettyFormat);
        configService.publishConfig(dataId, group, content, ConfigType.JSON.getType());
        PUBLISH_SNAPSHOTS.put(snapshotKey(dataId, group), new PublishSnapshot(content, System.currentTimeMillis()));
    }

    /**
     * 读取配置内容：刚发布内容的快照仍有效时（见 {@link #SNAPSHOT_TTL_MS}）
     * 直接返回快照，否则回退到 {@link ConfigService#getConfig}
     */
    public static String readFreshConfig(ConfigService configService, String appName,
            String dataIdPostfix, String group, long timeoutMs) throws NacosException {
        String dataId = appName + dataIdPostfix;
        long now = System.currentTimeMillis();
        PublishSnapshot snapshot = PUBLISH_SNAPSHOTS.get(snapshotKey(dataId, group));
        if (snapshot != null && now - snapshot.publishTime < SNAPSHOT_TTL_MS) {
            return snapshot.content;
        }
        return configService.getConfig(dataId, group, timeoutMs);
    }

    /** 生成 {@code dataId@group} 形式的快照缓存 key */
    private static String snapshotKey(String dataId, String group) {
        return dataId + "@" + group;
    }

    /** 私有构造方法，防止实例化 */
    private NacosConfigUtil() {}
}
