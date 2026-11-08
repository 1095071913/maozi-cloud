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
 * 地区实体类
 * <p>
 * 对应数据库表 system_config_region，存储省、市、区等地区信息。
 * 继承自 AbstractBaseNameDomain，包含基础名称字段。
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
@TableName("system_config_region")
public class RegionDo extends AbstractBaseNameDomain {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 父级ID */
	private Long parentId;

	/** 简称 */
	private String sname;

	/** 级别 */
	private Integer level;

	/** 城市编码 */
	private String cityCode;

	/** 邮政编码 */
	private String mailCode;

	/** 组合名称 */
	private String mername;

	/** 经度 */
	private Float Lng;

	/** 纬度 */
	private Float Lat;

	/** 拼音 */
	private String pinyin;

}
