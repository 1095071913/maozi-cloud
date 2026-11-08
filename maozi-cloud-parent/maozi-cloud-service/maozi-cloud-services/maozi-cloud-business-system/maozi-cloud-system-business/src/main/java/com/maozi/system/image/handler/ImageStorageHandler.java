package com.maozi.system.image.handler;

import com.maozi.system.image.enums.ImageStorageType;
import com.maozi.system.image.result.ImageUploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片存储处理器接口
 * <p>
 * 图片存储的策略抽象，每种存储渠道（阿里云 OSS、本地存储等）
 * 提供一个实现类，由配置项 oss.store-type
 * 决定上传请求实际路由到的处理器。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public interface ImageStorageHandler {

	/**
	 * 获取处理器支持的存储类型
	 *
	 * @return 图片存储类型枚举
	 */
	ImageStorageType getType();

	/**
	 * 上传图片
	 *
	 * @param file 上传的图片文件
	 * @param path 保存路径（对象存储目录前缀）
	 * @return 上传结果，包含对象存储路径与访问地址等信息
	 */
	ImageUploadResult upload(MultipartFile file,String path);

}
