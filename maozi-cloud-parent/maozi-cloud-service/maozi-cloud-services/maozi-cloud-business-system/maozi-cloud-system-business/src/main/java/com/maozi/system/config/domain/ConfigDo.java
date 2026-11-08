package com.maozi.system.config.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maozi.db.domain.AbstractBaseNameDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 全局配置实体类
 * <p>
 * 对应数据库表 system_config，存储系统全局配置信息。
 * 继承自 AbstractBaseNameDomain，配置名称（name）作为全局唯一键。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class ConfigDo extends AbstractBaseNameDomain {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 配置别名 */
	private String alias;

	/** 配置类型 */
	private String type;

	/** 配置值 */
	private String value;

	/** 排序序号，值越小越靠前 */
	private Integer sort;

}
