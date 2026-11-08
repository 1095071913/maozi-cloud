package com.maozi.base.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 批量新增/更新参数基类
 * <p>
 * 批量操作时的公共参数对象，仅包含主键标识字段。
 * 子类可扩展具体的业务字段。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveUpdateBatch implements Serializable {

	/** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 数据主键标识 */
	@Schema(description = "标识")
	private Long id;

}
