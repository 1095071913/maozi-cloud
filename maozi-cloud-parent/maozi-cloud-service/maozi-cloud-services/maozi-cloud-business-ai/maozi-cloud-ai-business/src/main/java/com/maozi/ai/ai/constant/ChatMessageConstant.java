package com.maozi.ai.ai.constant;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话消息元数据常量
 * <p>
 * 定义存储在消息元数据（metadata）中的属性 key。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/23
 */
public class ChatMessageConstant implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息 ID（雪花 ID）在消息元数据中的 key */
    public static final String ID = "messageId";

    /** 消息写入时刻毫秒时间戳在消息元数据中的 key */
    public static final String CREATE_TIME = "createTime";

}
