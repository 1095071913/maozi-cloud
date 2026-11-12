package com.maozi.base.param.plugin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 时间范围查询参数
 * <p>
 * 用于时间范围查询的公共参数对象，包含开始时间和结束时间（时间戳格式）。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeParam implements Serializable {

	/** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 开始时间（时间戳） */
	@Schema(description = "开始时间 时间戳")
	private Long startTime;

	/** 结束时间（时间戳） */
	@Schema(description = "结束时间 时间戳")
	private Long endTime;

}
