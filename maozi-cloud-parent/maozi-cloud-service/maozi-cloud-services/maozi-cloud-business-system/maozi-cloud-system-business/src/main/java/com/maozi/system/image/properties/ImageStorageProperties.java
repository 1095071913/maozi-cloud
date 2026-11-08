package com.maozi.system.image.properties;

import com.maozi.system.image.enums.ImageStorageType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片存储配置属性
 * <p>
 * 对应配置文件中 "oss" 前缀下的配置项，
 * store-type 决定图片上传路由到的存储处理器，
 * aliyun 为阿里云 OSS 处理器所需的连接配置。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@Configuration
@ConfigurationProperties("oss")
public class ImageStorageProperties implements Serializable {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 图片存储类型（对应 ImageStorageType 的 code，如 aliyun），默认 aliyun */
	private String storeType = ImageStorageType.ALIYUN_OSS.getCode();

	/** 阿里云 OSS 配置 */
	private Aliyun aliyun = new Aliyun();

	/**
	 * 阿里云 OSS 配置项
	 * <p>
	 * 包含访问凭证、地域、访问域名、存储空间等连接 OSS 所需的配置。
	 * </p>
	 *
	 * @author maozi
	 */
	@Data
	public static class Aliyun implements Serializable {
		/** 序列化标识 */
		@Serial
		private static final long serialVersionUID = 1L;

		/** 访问凭证 AccessKey ID */
		private String accessKeyId;

		/** 访问凭证 AccessKey Secret */
		private String accessKeySecret;

		/** 地域（Region），如 cn-hangzhou */
		private String region;

		/** 访问域名（Endpoint），如 https://oss-cn-hangzhou.aliyuncs.com，未配置时由地域自动解析 */
		private String endpoint;

		/** 存储空间（Bucket）名称 */
		private String bucket;

		/** 自定义访问域名（可选），如 CDN 加速域名，配置后文件访问地址基于该域名生成 */
		private String domain;

	}

}
