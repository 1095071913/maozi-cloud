package com.maozi.ai.ai.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * AI对话消息类型枚举
 * <p>
 * 标识对话消息的来源，分为用户消息与AI消息两种类型。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/17 10:09
 */
@Schema(description = "AI对话消息类型",type = "integer")
public enum ChatMessageType {

    /** 用户消息 */
    USER(0,"用户消息"),

    /** AI消息 */
    AI(1,"AI消息")

    ;

    /**
     * 构造方法
     *
     * @param value AI对话消息类型的数值编码
     * @param desc  AI对话消息类型的中文描述
     */
    ChatMessageType(Integer value,String desc) {

        this.value = value;

        this.desc = desc;

    }

    /** AI对话消息类型的数值编码 */
    @Getter
    private final Integer value;

    /** AI对话消息类型的中文描述 */
    @Getter
    private final String desc;

    /**
     * 重写toString方法，返回 "编码.描述" 格式的字符串
     *
     * @return 格式化的枚举字符串表示
     */
    @Override
    public String toString() {
        return value + "." + desc;
    }

}
