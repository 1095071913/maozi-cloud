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
package com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.BaseRuleEntity;
import com.alibaba.csp.sentinel.slots.block.Rule;

import java.util.Objects;

/**
 * {@link GatewayFlowRule} 对应的网关流控规则实体。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class GatewayFlowRuleEntity extends BaseRuleEntity {

    /** 统计窗口单位：秒。 */
    public static final int INTERVAL_UNIT_SECOND = 0;
    /** 统计窗口单位：分。 */
    public static final int INTERVAL_UNIT_MINUTE = 1;
    /** 统计窗口单位：时。 */
    public static final int INTERVAL_UNIT_HOUR = 2;
    /** 统计窗口单位：天。 */
    public static final int INTERVAL_UNIT_DAY = 3;

    /** 每分钟秒数，用于统计窗口单位换算。 */
    private static final long SECONDS_PER_MINUTE = 60;
    /** 每小时秒数，用于统计窗口单位换算。 */
    private static final long SECONDS_PER_HOUR = 60 * 60;
    /** 每天秒数，用于统计窗口单位换算。 */
    private static final long SECONDS_PER_DAY = 60 * 60 * 24;

    /** 规则作用的资源名（依 resourceMode 为 Route ID 或 API 名称）。 */
    private String resource;
    /** 资源模式：0-Route ID、1-自定义 API 名称。 */
    private Integer resourceMode;

    /** 阈值类型：0-线程数、1-QPS。 */
    private Integer grade;
    /** 阈值数量。 */
    private Double count;
    /** 统计窗口时长，单位由 intervalUnit 决定。 */
    private Long interval;
    /** 统计窗口时长单位：0-秒、1-分、2-时、3-天。 */
    private Integer intervalUnit;

    /** 流控效果：0-默认、2-匀速排队。 */
    private Integer controlBehavior;
    /** 超出阈值时允许的突发增量。 */
    private Integer burst;

    /** 匀速排队模式下的最大排队超时时间（毫秒）。 */
    private Integer maxQueueingTimeoutMs;

    /** 参数限流条目，非参数限流时为 null。 */
    private GatewayParamFlowItemEntity paramItem;

    /**
     * 将指定窗口时长及单位（0-秒、1-分、2-时、3-天）换算为秒数，
     * 未知单位抛出 IllegalArgumentException。
     */
    public static Long calIntervalSec(Long interval, Integer intervalUnit) {
        switch (intervalUnit) {
            case INTERVAL_UNIT_SECOND:
                return interval;
            case INTERVAL_UNIT_MINUTE:
                return interval * SECONDS_PER_MINUTE;
            case INTERVAL_UNIT_HOUR:
                return interval * SECONDS_PER_HOUR;
            case INTERVAL_UNIT_DAY:
                return interval * SECONDS_PER_DAY;
            default:
                break;
        }

        throw new IllegalArgumentException("Invalid intervalUnit: " + intervalUnit);
    }

    /**
     * 将秒数解析为 [interval, intervalUnit] 数组，优先取能整除的最大单位
     * （依次为天、时、分、秒）。
     */
    public static Object[] parseIntervalSec(Long intervalSec) {
        if (intervalSec % SECONDS_PER_DAY == 0) {
            return new Object[] {intervalSec / SECONDS_PER_DAY, INTERVAL_UNIT_DAY};
        }

        if (intervalSec % SECONDS_PER_HOUR == 0) {
            return new Object[] {intervalSec / SECONDS_PER_HOUR, INTERVAL_UNIT_HOUR};
        }

        if (intervalSec % SECONDS_PER_MINUTE == 0) {
            return new Object[] {intervalSec / SECONDS_PER_MINUTE, INTERVAL_UNIT_MINUTE};
        }

        return new Object[] {intervalSec, INTERVAL_UNIT_SECOND};
    }

    /**
     * 转换为 {@link GatewayFlowRule}：intervalSec 由 interval 与 intervalUnit 换算得出，
     * burst、maxQueueingTimeoutMs 及 paramItem 的 matchStrategy 仅在非空时设置。
     */
    public GatewayFlowRule toGatewayFlowRule() {
        GatewayFlowRule rule = new GatewayFlowRule();
        rule.setResource(resource);
        rule.setResourceMode(resourceMode);

        rule.setGrade(grade);
        rule.setCount(count);
        rule.setIntervalSec(calIntervalSec(interval, intervalUnit));

        rule.setControlBehavior(controlBehavior);

        if (burst != null) {
            rule.setBurst(burst);
        }

        if (maxQueueingTimeoutMs != null) {
            rule.setMaxQueueingTimeoutMs(maxQueueingTimeoutMs);
        }

        if (paramItem != null) {
            GatewayParamFlowItem ruleItem = new GatewayParamFlowItem();
            rule.setParamItem(ruleItem);
            ruleItem.setParseStrategy(paramItem.getParseStrategy());
            ruleItem.setFieldName(paramItem.getFieldName());
            ruleItem.setPattern(paramItem.getPattern());

            // 缓存匹配策略，避免同一方法内重复调用
            Integer matchStrategy = paramItem.getMatchStrategy();
            if (matchStrategy != null) {
                ruleItem.setMatchStrategy(matchStrategy);
            }
        }

        return rule;
    }

    /**
     * 由指定 GatewayFlowRule 构建实体，将 intervalSec 解析为 interval 与 intervalUnit。
     */
    public static GatewayFlowRuleEntity fromGatewayFlowRule(String app, String ip, Integer port, GatewayFlowRule rule) {
        GatewayFlowRuleEntity entity = new GatewayFlowRuleEntity();
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);

        entity.setResource(rule.getResource());
        entity.setResourceMode(rule.getResourceMode());

        entity.setGrade(rule.getGrade());
        entity.setCount(rule.getCount());
        Object[] intervalSecResult = parseIntervalSec(rule.getIntervalSec());
        entity.setInterval((Long) intervalSecResult[0]);
        entity.setIntervalUnit((Integer) intervalSecResult[1]);

        entity.setControlBehavior(rule.getControlBehavior());
        entity.setBurst(rule.getBurst());
        entity.setMaxQueueingTimeoutMs(rule.getMaxQueueingTimeoutMs());

        GatewayParamFlowItem paramItem = rule.getParamItem();
        if (paramItem != null) {
            GatewayParamFlowItemEntity itemEntity = new GatewayParamFlowItemEntity();
            entity.setParamItem(itemEntity);
            itemEntity.setParseStrategy(paramItem.getParseStrategy());
            itemEntity.setFieldName(paramItem.getFieldName());
            itemEntity.setPattern(paramItem.getPattern());
            itemEntity.setMatchStrategy(paramItem.getMatchStrategy());
        }

        return entity;
    }

    /** 始终返回 null，因为网关流控规则不是通用 Sentinel 规则。 */
    @Override
    public Rule toRule() {
        return null;
    }

    /** 获取参数限流条目，非参数限流时为 null。 */
    public GatewayParamFlowItemEntity getParamItem() {
        return paramItem;
    }

    /** 设置参数限流条目。 */
    public void setParamItem(GatewayParamFlowItemEntity paramItem) {
        this.paramItem = paramItem;
    }

    /** 获取规则作用的资源名。 */
    public String getResource() {
        return resource;
    }

    /** 设置规则作用的资源名。 */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** 获取资源模式：0-Route ID、1-自定义 API 名称。 */
    public Integer getResourceMode() {
        return resourceMode;
    }

    /** 设置资源模式：0-Route ID、1-自定义 API 名称。 */
    public void setResourceMode(Integer resourceMode) {
        this.resourceMode = resourceMode;
    }

    /** 获取阈值类型：0-线程数、1-QPS。 */
    public Integer getGrade() {
        return grade;
    }

    /** 设置阈值类型：0-线程数、1-QPS。 */
    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    /** 获取阈值数量。 */
    public Double getCount() {
        return count;
    }

    /** 设置阈值数量。 */
    public void setCount(Double count) {
        this.count = count;
    }

    /** 获取统计窗口时长，单位由 intervalUnit 决定。 */
    public Long getInterval() {
        return interval;
    }

    /** 设置统计窗口时长，单位由 intervalUnit 决定。 */
    public void setInterval(Long interval) {
        this.interval = interval;
    }

    /** 获取统计窗口时长单位。 */
    public Integer getIntervalUnit() {
        return intervalUnit;
    }

    /** 设置统计窗口时长单位。 */
    public void setIntervalUnit(Integer intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    /** 获取流控效果。 */
    public Integer getControlBehavior() {
        return controlBehavior;
    }

    /** 设置流控效果。 */
    public void setControlBehavior(Integer controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    /** 获取超出阈值时允许的突发增量。 */
    public Integer getBurst() {
        return burst;
    }

    /** 设置超出阈值时允许的突发增量。 */
    public void setBurst(Integer burst) {
        this.burst = burst;
    }

    /** 获取最大排队超时时间（毫秒）。 */
    public Integer getMaxQueueingTimeoutMs() {
        return maxQueueingTimeoutMs;
    }

    /** 设置最大排队超时时间（毫秒）。 */
    public void setMaxQueueingTimeoutMs(Integer maxQueueingTimeoutMs) {
        this.maxQueueingTimeoutMs = maxQueueingTimeoutMs;
    }

    /** 判断指定对象是否与本实体全部字段相同。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        GatewayFlowRuleEntity that = (GatewayFlowRuleEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(app, that.app) &&
                Objects.equals(ip, that.ip) &&
                Objects.equals(port, that.port) &&
                Objects.equals(gmtCreate, that.gmtCreate) &&
                Objects.equals(gmtModified, that.gmtModified) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(resourceMode, that.resourceMode) &&
                Objects.equals(grade, that.grade) &&
                Objects.equals(count, that.count) &&
                Objects.equals(interval, that.interval) &&
                Objects.equals(intervalUnit, that.intervalUnit) &&
                Objects.equals(controlBehavior, that.controlBehavior) &&
                Objects.equals(burst, that.burst) &&
                Objects.equals(maxQueueingTimeoutMs, that.maxQueueingTimeoutMs) &&
                Objects.equals(paramItem, that.paramItem);
    }

    /** 以全部字段计算哈希码。 */
    @Override
    public int hashCode() {
        return Objects.hash(id, app, ip, port, gmtCreate, gmtModified, resource, resourceMode, grade, count, interval, 
            intervalUnit, controlBehavior, burst, maxQueueingTimeoutMs, paramItem);
    }

    /** 拼接全部字段构建字符串。 */
    @Override
    public String toString() {
        return "GatewayFlowRuleEntity{" +
                "id=" + id +
                ", app='" + app + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                ", resource='" + resource + '\'' +
                ", resourceMode=" + resourceMode +
                ", grade=" + grade +
                ", count=" + count +
                ", interval=" + interval +
                ", intervalUnit=" + intervalUnit +
                ", controlBehavior=" + controlBehavior +
                ", burst=" + burst +
                ", maxQueueingTimeoutMs=" + maxQueueingTimeoutMs +
                ", paramItem=" + paramItem +
                '}';
    }
}
