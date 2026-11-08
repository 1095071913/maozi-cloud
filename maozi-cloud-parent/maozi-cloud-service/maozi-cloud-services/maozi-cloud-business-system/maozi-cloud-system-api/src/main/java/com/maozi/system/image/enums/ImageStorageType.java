package com.maozi.system.image.enums;

import com.maozi.common.enums.BaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 图片存储类型枚举
 * <p>
 * 用于标识图片上传后采用的存储渠道，配置项 oss.store-type
 * 通过 {@link #getCode()} 与本枚举匹配，从而路由到对应的存储处理器。
 * 当前支持阿里云对象存储（OSS），后续可扩展本地存储、MinIO 等类型。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Schema(description = "图片存储类型",type = "integer")
public enum ImageStorageType implements BaseEnum {

	/** 阿里云对象存储 */
	ALIYUN_OSS(0,"aliyun","阿里云OSS"),

	;

	/**
	 * 构造方法
	 *
	 * @param value 存储类型的数值编码
	 * @param code  存储类型的字符串编码，与配置文件中的 store-type 值对应
	 * @param desc  存储类型的中文描述
	 */
	ImageStorageType(Integer value,String code,String desc) {

		this.value = value;

		this.code = code;

		this.desc = desc;

	}

	/** 存储类型的数值编码 */
	@Getter
	private final Integer value;

	/** 存储类型的字符串编码，与配置文件中的 store-type 值对应 */
	@Getter
	private final String code;

	/** 存储类型的中文描述 */
	@Getter
	private final String desc;

	/**
	 * 根据字符串编码获取存储类型枚举
	 *
	 * @param code 存储类型的字符串编码（如 aliyun），忽略大小写
	 * @return 对应的存储类型枚举，未匹配时返回 null
	 */
	public static ImageStorageType get(String code) {

		for(ImageStorageType type : values()) {
			if(code != null && type.code.equalsIgnoreCase(code)) {
				return type;
			}
		}

		return null;

	}

	/**
	 * 输出枚举的字符串表示
	 *
	 * @return 格式为 "数值编码.描述" 的字符串，例如 "0.阿里云OSS"
	 */
	@Override
	public String toString() {
		return value + "." + desc;
	}

}
