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
 * 继承 ImageServiceImpl 并实现 RestImageService 接口，提供图片上传的 RESTful 接口。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestImageServiceImpl extends ImageServiceImpl implements RestImageService {

	/**
	 * 构造方法
	 *
	 * @param handlers 容器内的存储处理器列表，由父类按存储类型建立索引
	 */
	public RestImageServiceImpl(List<ImageStorageHandler> handlers) {
		super(handlers);
	}

	/**
	 * 上传图片
	 *
	 * @param param 图片上传参数，包含图片文件列表与保存路径
	 * @return 上传结果列表，每张图片对应一项
	 */
	@Override
	public AbstractBaseResult<List<ImageUploadResult>> uploadResult(ImageUploadParam param) {
		return ResultUtil.success(super.upload(param));
	}

}
