package com.maozi.system.config.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统属性视图对象
 * <p>
 * 用于展示系统的基础配置信息，数据来源于系统属性配置（SystemProperties），
 * 包括项目名称、公司名称、图标、运行环境、项目描述和版权声明等。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
public class SystemPropertiesResult implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 项目名称 */
	@Schema(description = "项目名称")
	private String projectName;

	/** 公司名称 */
	@Schema(description = "公司名称")
	private String corporationName;

	/** 项目图标 */
	@Schema(description = "项目图标")
	private String icon;

	/** 项目运行环境（如dev/test/prod） */
	@Schema(description = "项目环境")
	private String environment;

	/** 项目描述信息 */
	@Schema(description = "项目描述")
	private String description;

	/** 项目版权声明 */
	@Schema(description = "项目版权")
	private String copyright;

}
