package com.maozi.system.image.api;

import com.maozi.system.image.param.ImageUploadParam;
import com.maozi.system.image.result.ImageUploadResult;

import java.util.List;

/**
 * 图片服务接口
 * <p>
 * 定义图片上传的业务能力，根据配置的存储类型
 * 路由到对应的存储处理器完成实际上传，
 * 支持单次上传一个或多个图片文件。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface ImageService {

	/**
	 * 上传图片
	 * <p>
	 * 根据配置项 oss.store-type 匹配存储类型，
	 * 路由到对应的存储处理器处理上传，
	 * 保存路径作为目录前缀传递给处理器。
	 * </p>
	 *
	 * @param param 图片上传参数，包含上传的图片文件列表与保存路径（目录前缀）
	 * @return 上传结果列表，每张图片对应一项，包含文件原始名称、对象存储路径、访问地址与文件大小
	 */
	List<ImageUploadResult> upload(ImageUploadParam param);

}
