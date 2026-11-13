package com.maozi.ai.ai.api.rest;

import com.maozi.ai.ai.param.ChatConversationRecordListParam;
import com.maozi.ai.ai.result.ChatConversationRecordListResult;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.annotation.Post;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI会话管理服务 REST 接口
 * <p>
 * 提供AI对话会话的 RESTful API 接口定义，
 * 包括当前用户的会话分页列表查询、会话创建、
 * 会话标题更新以及会话删除等操作。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/16 21:35
 */
@Tag(name = "AI会话模块")
public interface RestChatConversationRecord {

    /** 接口基础路径 */
    String PATH = "/chat/record";

    /**
     * 查询当前用户的会话分页列表
     *
     * @param pageParam 分页查询参数，包含页码、每页数量以及会话列表的排序条件
     * @return 返回会话分页列表数据，包含会话 ID、标题等信息
     */
    @Post(value = PATH + "/list", description = "列表")
    AbstractBaseResult<PageResult<ChatConversationRecordListResult>> restList(@RequestBody PageParam<ChatConversationRecordListParam> pageParam);

    /**
     * 创建会话
     *
     * @param param 携带首条消息内容的请求参数，该消息内容作为会话标题
     * @return 返回新建会话的 ID
     */
    @Post(value = PATH + "/create", description = "保存")
    AbstractBaseResult<Long> restCreate(@RequestBody RequestParam<String> param);


    /** 当前会话基础路径，携带会话 ID 路径变量 */
    String CURRENT_PATH = PATH + "/{conversationId}";

    /**
     * 更新会话标题
     *
     * @param conversationId 会话 ID
     * @param param 携带新标题的请求参数
     * @return 空结果
     */
    @Post(value = CURRENT_PATH + "/updateTitle", description = "更新标题")
    AbstractBaseResult<Void> restUpdateTitle(@PathVariable Long conversationId, @RequestBody RequestParam<String> param);

    /**
     * 删除会话
     *
     * @param conversationId 会话 ID
     * @return 空结果
     */
    @Post(value = CURRENT_PATH + "/remove", description = "删除")
    AbstractBaseResult<Void> restRemove(@PathVariable String conversationId);

}
