package com.maozi.ai.ai.result;

import com.maozi.ai.ai.enums.ChatType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI流式对话返回结果
 * <p>
 * 对话流中的单个分片：OUTPUT 表示持续对话中的回复内容分片，
 * FINISH 表示对话流已结束，前端可关闭连接。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/16 18:44
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResult implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 对话流状态类型：OUTPUT 持续输出中 / FINISH 对话已结束 */
    @Schema(description = "AI对话类型")
    private ChatType type;

    /** 当前分片的回复内容 */
    @Schema(description = "消息")
    private String message;

    /** 附加数据（预留字段，当前流式对话未写入该字段） */
    @Schema(description = "数据")
    private Object data;

    /**
     * 自定义构造方法，构建仅包含类型与消息内容、不含附加数据的返回结果
     *
     * @param type    AI对话流状态类型
     * @param message 消息内容
     */
    public ChatResult(ChatType type, String message){
        this.type = type;
        this.message = message;
    }

}
