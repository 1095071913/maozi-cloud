package com.maozi.ai.ai.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * AI对话流状态类型枚举
 * <p>
 * 用于流式对话返回结果中标识当前对话所处的状态：
 * OUTPUT 表示AI正在持续输出回复内容，FINISH 表示对话已结束。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/16 18:45
 */
@Schema(description = "AI对话类型",type = "integer")
public enum ChatType {

    /** 完成对话（对话流已结束） */
    FINISH(0,"完成对话"),

    /** 持续对话中（AI 正在输出回复内容） */
    OUTPUT(1,"持续对话中"),

    ;

    /**
     * 构造方法
     *
     * @param value AI对话类型的数值编码
     * @param desc  AI对话类型的中文描述
     */
    ChatType(Integer value,String desc) {

		this.value = value;

		this.desc = desc;

    }

    /** AI对话类型的数值编码 */
    @Getter
    private final Integer value;

    /** AI对话类型的中文描述 */
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
