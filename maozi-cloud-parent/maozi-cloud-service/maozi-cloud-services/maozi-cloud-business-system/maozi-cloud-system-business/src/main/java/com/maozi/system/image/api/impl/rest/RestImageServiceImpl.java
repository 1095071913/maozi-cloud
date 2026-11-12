package com.maozi.system.image.api.impl.rest;

import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.system.image.api.impl.ImageServiceImpl;
import com.maozi.system.image.api.rest.RestImageService;
import com.maozi.system.image.handler.ImageStorageHandler;
import com.maozi.system.image.param.ImageUploadParam;
import com.maozi.system.image.result.ImageUploadResult;

import java.util.List;

/**
 * 图片模块 REST 服务实现类
 * <p>
 * 继承自 ImageServiceImpl，实现 RestImageService 接口，
 * 提供图片上传的 RESTful API 接口实现，
 * 上传请求根据配置路由到对应的存储处理器处理。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestImageServiceImpl extends ImageServiceImpl implements RestImageService {

	/**
	 * 构造方法
	 * <p>
	 * 将容器内的存储处理器列表传递给父类，
	 * 由父类按存储类型建立索引供上传路由使用。
	 * </p>
	 *
	 * @param handlers 容器内的存储处理器列表
	 */
	public RestImageServiceImpl(List<ImageStorageHandler> handlers) {
		super(handlers);
	}

	/**
	 * 上传图片
	 *
	 * @param param 图片上传参数，包含上传的图片文件列表与保存路径（目录前缀）
	 * @return 上传结果列表，每张图片对应一项，包含文件原始名称、对象存储路径、访问地址与文件大小
	 */
	@Override
	public AbstractBaseResult<List<ImageUploadResult>> restUpload(ImageUploadParam param) {
		return ResultUtil.success(upload(param));
	}

}
