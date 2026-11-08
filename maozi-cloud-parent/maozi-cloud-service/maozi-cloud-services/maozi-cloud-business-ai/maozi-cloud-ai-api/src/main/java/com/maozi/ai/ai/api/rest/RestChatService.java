package com.maozi.ai.ai.api.rest;

import com.maozi.ai.ai.param.ChatGenerateImageParam;
import com.maozi.ai.ai.param.ChatParam;
import com.maozi.ai.ai.vo.ChatGenerateImageResult;
import com.maozi.ai.ai.vo.ChatItemResult;
import com.maozi.ai.ai.vo.ChatListResult;
import com.maozi.ai.ai.vo.ChatResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.annotation.Get;
import com.maozi.service.annotation.Post;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI对话服务 REST 接口
 * <p>
 * 提供AI对话的 RESTful API 接口定义，
 * 包括AI流式对话、文生图、对话消息列表查询、
 * 停止对话、增量获取消息以及删除指定消息等操作。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Tag(name = "AI对话模块")
public interface RestChatService {

	/** 接口基础路径 */
	String PATH = "/chat";

	/**
	 * AI流式对话
	 *
	 * @param param 对话参数，包含会话 ID、消息内容、图片列表（多模态对话）以及提示词配置 key
	 * @return 返回 AI 回复的分片流（SSE），最后追加一条对话完成（FINISH）消息
	 */
	@Post(value = PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE, description = "发起")
    Flux<AbstractBaseResult<ChatResult>> chat(@RequestBody @Valid ChatParam param);

	/**
	 * AI文生图
	 *
	 * @param param 文生图参数，包含会话 ID、图片描述内容、生成数量以及图片宽高
	 * @return 返回提示消息与生成图片的地址列表
	 */
	@Post(value = PATH + "/generate/image", description = "发起图片生成")
	AbstractBaseResult<ChatGenerateImageResult> generateImage(@RequestBody @Valid ChatGenerateImageParam param);


	/** 当前会话基础路径，携带会话 ID 路径变量 */
	String CURRENT_PATH = PATH + "/{conversationId}";

	/**
	 * 查询会话的对话消息列表
	 *
	 * @param conversationId 会话 ID
	 * @return 返回会话是否正在对话中以及该会话的对话消息列表
	 */
	@Get(value = CURRENT_PATH + "/list", description = "列表")
	AbstractBaseResult<ChatListResult> list(@PathVariable String conversationId);

	/**
	 * 停止会话进行中的对话或文生图
	 *
	 * @param conversationId 会话 ID
	 * @return 空结果
	 */
	@Post(value = CURRENT_PATH + "/stop", description = "停止")
	AbstractBaseResult<Void> stop(@PathVariable String conversationId);

	/**
	 * 查询会话中指定时间之后的消息列表
	 *
	 * @param conversationId 会话 ID
	 * @param createTime 时间毫秒值，查询写入时间晚于该值的消息（不含等于）
	 * @return 返回该时间之后写入的增量消息列表
	 */
	@Get(value = CURRENT_PATH + "/getAfterMessages", description = "获取后续消息列表")
	AbstractBaseResult<List<ChatItemResult>> getAfterMessages(@PathVariable String conversationId, @RequestParam Long createTime);

	/**
	 * 删除会话中的指定消息
	 *
	 * @param conversationId 会话 ID
	 * @param messageId 消息 ID
	 * @return 空结果
	 */
	@Post(value = CURRENT_PATH + "/{messageId}/remove", description = "删除消息")
	AbstractBaseResult<Void> removeMessage(@PathVariable String conversationId, @PathVariable String messageId);

}
