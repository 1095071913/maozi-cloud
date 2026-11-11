package com.maozi.ai.ai.param;

import com.maozi.base.param.plugin.OrderParam;
import com.maozi.base.plugin.context.QueryEnvironmentContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * AI会话列表查询参数
 * <p>
 * 实现通用排序参数接口，定义会话记录分页查询的排序规则，
 * 支持前端传入排序字段与排序方向，未传入时默认按创建时间倒序排列。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/17 09:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationRecordListParam implements OrderParam, Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 排序字段映射，key为字段名，value为排序方向（true 降序、false 升序、null 不排序） */
    private Map<String, Boolean> orderFieldMap;

    /** 升序排序字段映射，key为表别名，value为该表下需升序排序的字段列表 */
    private Map<String, List<String>> orderAscFieldsMap;

    /** 降序排序字段映射，key为表别名，value为该表下需降序排序的字段列表 */
    private Map<String, List<String>> orderDescFieldsMap;

    /** 主排序字段映射，key为默认表别名，value为可排序字段及默认排序方向，默认按创建时间倒序排列 */
    private final Map<String, Map<String, Boolean>> orderMainFieldsMap = Map.of(
        QueryEnvironmentContext.DEFAULT_ORDER_KEY, Map.of(
            CREATE_TIME_KEY, true
        )
    );

}
