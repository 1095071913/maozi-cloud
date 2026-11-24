package com.maozi.system.config.param;

import com.maozi.base.param.plugin.OrderParam;
import com.maozi.base.plugin.context.QueryEnvironmentContext;
import com.maozi.base.plugin.query.QueryBaseTypeEnum;
import com.maozi.base.plugin.query.QueryPlugin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 全局配置列表查询参数
 * <p>
 * 用于全局配置分页查询时的请求参数封装。
 * 支持按配置类型精确查询、按配置名称与配置别名模糊查询，
 * 并实现了排序参数接口以支持自定义排序，默认按排序值升序、创建时间倒序排列。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
public class ConfigListParam implements OrderParam, Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 默认升序排序字段名（排序值字段） */
    public static final String SORT_KEY = "sort";

    /** 配置名称（模糊查询） */
    @Schema(description = "名称")
    @QueryPlugin(value = QueryBaseTypeEnum.LIKE)
    private String name;

    /** 配置别名（模糊查询） */
    @Schema(description = "别名")
    @QueryPlugin(value = QueryBaseTypeEnum.LIKE)
    private String alias;

    /** 配置类型（精确查询） */
    @Schema(description = "类型")
    @NotEmpty(message = "类型不能为空")
    @QueryPlugin(value = QueryBaseTypeEnum.EQ)
    private String type;

    /** 排序字段映射，key为字段名，value为排序方向（true 降序、false 升序、null 不排序） */
    private Map<String, Boolean> orderFieldMap;

    /** 升序排序字段映射，key为表别名，value为该表下需要升序排序的字段列表 */
    private Map<String, List<String>> orderAscFieldsMap;

    /** 降序排序字段映射，key为表别名，value为该表下需要降序排序的字段列表 */
    private Map<String, List<String>> orderDescFieldsMap;

    /** 主表可排序字段映射，key为表别名，value为该表下可排序字段及默认排序方向
     * （true 降序、false 升序），未设置时按默认规则兜底 */
    private Map<String, Map<String, Boolean>> orderMainFieldsMap;

    /**
     * 获取主表可排序字段映射，未设置时按默认规则兜底（排序值升序、创建时间倒序）
     *
     * @return 主表可排序字段映射
     */
    @Override
    public Map<String, Map<String, Boolean>> getOrderMainFieldsMap() {
        if (orderMainFieldsMap == null) {
            orderMainFieldsMap = Map.of(QueryEnvironmentContext.defaultOrderKey,
                    Map.of(SORT_KEY, Boolean.FALSE, CREATE_TIME_KEY, Boolean.TRUE));
        }
        return orderMainFieldsMap;
    }

}
