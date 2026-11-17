package com.maozi.ai.ai.api;

/**
 * AI对话业务接口
 * <p>
 * 当前未定义对内业务方法，AI 对话能力统一经 REST 接口层
 * （RestChatMessageService）对外暴露；实现类 ChatServiceImpl 提供
 * 对话消息的公共转换能力，供 REST 层实现类继承复用。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/16 10:04
 */
public interface ChatService {
}
