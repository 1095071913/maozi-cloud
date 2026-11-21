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
 * Entity for {@link GatewayFlowRule}.
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class GatewayFlowRuleEntity extends BaseRuleEntity {

    /** Interval unit: second. */
    public static final int INTERVAL_UNIT_SECOND = 0;
    /** Interval unit: minute. */
    public static final int INTERVAL_UNIT_MINUTE = 1;
    /** Interval unit: hour. */
    public static final int INTERVAL_UNIT_HOUR = 2;
    /** Interval unit: day. */
    public static final int INTERVAL_UNIT_DAY = 3;

    /** The resource name the rule applies to (route ID or API name depending on resourceMode). */
    private String resource;
    /** Whether the resource is a route ID (0) or a custom API name (1). */
    private Integer resourceMode;

    /** The threshold type: 0 for thread count and 1 for QPS. */
    private Integer grade;
    /** The threshold count. */
    private Double count;
    /** The statistic interval, in the unit of intervalUnit. */
    private Long interval;
    /** The unit of the statistic interval: 0-second, 1-minute, 2-hour, 3-day. */
    private Integer intervalUnit;

    /** The control behavior: 0 default and 2 rate limiter. */
    private Integer controlBehavior;
    /** The burst count allowed when the threshold is exceeded. */
    private Integer burst;

    /** The max queueing timeout in milliseconds for rate limiter behavior. */
    private Integer maxQueueingTimeoutMs;

    /** The parameter flow item for parameter-based control, null if not parameter-based. */
    private GatewayParamFlowItemEntity paramItem;

    /**
     * Converts the given interval and unit (0-second, 1-minute, 2-hour, 3-day) to the
     * interval in seconds, throwing IllegalArgumentException for an unknown unit.
     */
    public static Long calIntervalSec(Long interval, Integer intervalUnit) {
        switch (intervalUnit) {
            case INTERVAL_UNIT_SECOND:
                return interval;
            case INTERVAL_UNIT_MINUTE:
                return interval * 60;
            case INTERVAL_UNIT_HOUR:
                return interval * 60 * 60;
            case INTERVAL_UNIT_DAY:
                return interval * 60 * 60 * 24;
            default:
                break;
        }

        throw new IllegalArgumentException("Invalid intervalUnit: " + intervalUnit);
    }

    /**
     * Parses the interval in seconds into an array of [interval, intervalUnit], preferring
     * the largest unit that divides it evenly (day, then hour, minute and second).
     */
    public static Object[] parseIntervalSec(Long intervalSec) {
        if (intervalSec % (60 * 60 * 24) == 0) {
            return new Object[] {intervalSec / (60 * 60 * 24), INTERVAL_UNIT_DAY};
        }

        if (intervalSec % (60 * 60 ) == 0) {
            return new Object[] {intervalSec / (60 * 60), INTERVAL_UNIT_HOUR};
        }

        if (intervalSec % 60 == 0) {
            return new Object[] {intervalSec / 60, INTERVAL_UNIT_MINUTE};
        }

        return new Object[] {intervalSec, INTERVAL_UNIT_SECOND};
    }

    /**
     * Converts to a {@link GatewayFlowRule}, deriving intervalSec from interval and
     * intervalUnit, and only setting burst, maxQueueingTimeoutMs and the matchStrategy of
     * paramItem when they are not null.
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

            if (paramItem.getMatchStrategy() != null) {
                ruleItem.setMatchStrategy(paramItem.getMatchStrategy());
            }
        }

        return rule;
    }

    /**
     * Builds an entity from the given GatewayFlowRule, parsing intervalSec into interval
     * and intervalUnit.
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

    /** Always returns null, as a gateway flow rule is not a common Sentinel rule. */
    @Override
    public Rule toRule() {
        return null;
    }

    /** Gets the parameter flow item, null if the rule is not parameter-based. */
    public GatewayParamFlowItemEntity getParamItem() {
        return paramItem;
    }

    /** Sets the parameter flow item for parameter-based control. */
    public void setParamItem(GatewayParamFlowItemEntity paramItem) {
        this.paramItem = paramItem;
    }

    /** Gets the resource name the rule applies to. */
    public String getResource() {
        return resource;
    }

    /** Sets the resource name the rule applies to. */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Gets the resource mode: route ID (0) or custom API name (1). */
    public Integer getResourceMode() {
        return resourceMode;
    }

    /** Sets the resource mode: route ID (0) or custom API name (1). */
    public void setResourceMode(Integer resourceMode) {
        this.resourceMode = resourceMode;
    }

    /** Gets the threshold type: 0 for thread count and 1 for QPS. */
    public Integer getGrade() {
        return grade;
    }

    /** Sets the threshold type: 0 for thread count and 1 for QPS. */
    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    /** Gets the threshold count. */
    public Double getCount() {
        return count;
    }

    /** Sets the threshold count. */
    public void setCount(Double count) {
        this.count = count;
    }

    /** Gets the statistic interval, in the unit of intervalUnit. */
    public Long getInterval() {
        return interval;
    }

    /** Sets the statistic interval, in the unit of intervalUnit. */
    public void setInterval(Long interval) {
        this.interval = interval;
    }

    /** Gets the unit of the statistic interval. */
    public Integer getIntervalUnit() {
        return intervalUnit;
    }

    /** Sets the unit of the statistic interval. */
    public void setIntervalUnit(Integer intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    /** Gets the control behavior. */
    public Integer getControlBehavior() {
        return controlBehavior;
    }

    /** Sets the control behavior. */
    public void setControlBehavior(Integer controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    /** Gets the burst count allowed when the threshold is exceeded. */
    public Integer getBurst() {
        return burst;
    }

    /** Sets the burst count allowed when the threshold is exceeded. */
    public void setBurst(Integer burst) {
        this.burst = burst;
    }

    /** Gets the max queueing timeout in milliseconds. */
    public Integer getMaxQueueingTimeoutMs() {
        return maxQueueingTimeoutMs;
    }

    /** Sets the max queueing timeout in milliseconds. */
    public void setMaxQueueingTimeoutMs(Integer maxQueueingTimeoutMs) {
        this.maxQueueingTimeoutMs = maxQueueingTimeoutMs;
    }

    /** Whether the given object has the same fields as this entity. */
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

    /** Gets the hash code from all fields of this entity. */
    @Override
    public int hashCode() {
        return Objects.hash(id, app, ip, port, gmtCreate, gmtModified, resource, resourceMode, grade, count, interval, 
            intervalUnit, controlBehavior, burst, maxQueueingTimeoutMs, paramItem);
    }

    /** Builds a string with all fields of this entity. */
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
