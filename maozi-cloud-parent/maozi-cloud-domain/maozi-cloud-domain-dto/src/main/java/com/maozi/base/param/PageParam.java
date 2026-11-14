package com.maozi.base.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询参数
 * <p>
 * 通用分页查询入参，包含页码、每页数量和查询条件。
 * 支持泛型，可搭配任意查询条件对象使用。
 * </p>
 *
 * @param <D> 查询条件数据类型
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageParam<D> implements Serializable {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页码（未传时默认取第 1 页，由分页转换处兜底） */
    @Schema(description = "页数",defaultValue = "1")
    private Long current;

    /** 每页数据条数（未传时默认每页 10 条，由分页转换处兜底） */
    @Schema(description = "每页数量",defaultValue = "10")
    private Long size;

    /** 查询条件数据 */
    @Valid
    @Schema(description = "查询条件")
    private D data;

}
