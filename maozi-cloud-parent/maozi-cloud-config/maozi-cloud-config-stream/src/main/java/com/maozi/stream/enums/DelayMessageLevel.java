package com.maozi.stream.enums;

import com.maozi.common.enums.BaseEnum;
import lombok.Getter;

/**
 * 延迟消息等级枚举
 * <p>
 * 用于 RocketMQ 延迟消息的延迟时间等级配置，共 18 个等级，
 * 对应从 1 秒到 2 小时的不同延迟时间。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public enum DelayMessageLevel implements BaseEnum {

    /** 延迟 1 秒 */
    ONE(0,"1s"),

    /** 延迟 5 秒 */
    TWO(1,"5s"),

    /** 延迟 10 秒 */
    THREE(2,"10s"),

    /** 延迟 30 秒 */
    FOUR(3,"30s"),

    /** 延迟 1 分钟 */
    FIVE(4,"1m"),

    /** 延迟 2 分钟 */
    SIX(5,"2m"),

    /** 延迟 3 分钟 */
    SEVEN(6,"3m"),

    /** 延迟 4 分钟 */
    EIGHT(7,"4m"),

    /** 延迟 5 分钟 */
    NINE(8,"5m"),

    /** 延迟 6 分钟 */
    TEN(9,"6m"),

    /** 延迟 7 分钟 */
    ELEVEN(10,"7m"),

    /** 延迟 8 分钟 */
    TWELVE(11,"8m"),

    /** 延迟 9 分钟 */
    THIRTEEN(12,"9m"),

    /** 延迟 10 分钟 */
    FOURTEEN(13,"10m"),

    /** 延迟 20 分钟 */
    FIFTEEN(14,"20m"),

    /** 延迟 30 分钟 */
    SIXTEEN(15,"30m"),

    /** 延迟 1 小时 */
    SEVENTEEN(16,"1h"),

    /** 延迟 2 小时 */
    EIGHTEEN(17,"2h"),

    ;

    /**
     * 构造方法
     *
     * @param value 枚举值（延迟级别序号，从 0 开始计数，发送时需换算为 RocketMQ 从 1 计数的延迟级别）
     * @param desc 延迟时间描述
     */
    DelayMessageLevel(Integer value,String desc) {

        this.value = value;

        this.desc = desc;

    }

    /** 枚举值 */
    @Getter
    private final Integer value;

    /** 枚举描述 */
    @Getter
    private final String desc;

    /**
     * 输出枚举的字符串表示
     *
     * @return 格式为 "值.描述" 的字符串
     */
    @Override
    public String toString() {
        return value + "." + desc;
    }

}
