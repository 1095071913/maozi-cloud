package com.maozi.system.image.api.impl;

import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.system.image.api.ImageService;
import com.maozi.system.image.enums.ImageStorageType;
import com.maozi.system.image.handler.ImageStorageHandler;
import com.maozi.system.image.param.ImageUploadParam;
import com.maozi.system.image.properties.ImageStorageProperties;
import com.maozi.system.image.result.ImageUploadResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 图片服务实现类
 * <p>
 * 实现图片上传的业务逻辑，构造时收集容器内全部
 * {@link ImageStorageHandler} 实现并按存储类型索引，
 * 上传时根据配置项 oss.store-type
 * 匹配存储类型并路由到对应的处理器处理，
 * 逐个文件调用处理器完成上传。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Service
public class ImageServiceImpl implements ImageService {

	/** 图片存储配置属性 */
	@Resource
	private ImageStorageProperties imageStorageProperties;

	/** 存储处理器索引，键为存储类型，值为对应处理器 */
	private final Map<ImageStorageType,ImageStorageHandler> handlerMap = new EnumMap<>(ImageStorageType.class);

	/**
	 * 构造方法
	 * <p>
	 * 收集容器内全部存储处理器实现，按其支持的存储类型建立索引，
	 * 供上传时按配置路由。
	 * </p>
	 *
	 * @param handlers 容器内的存储处理器列表
	 */
	public ImageServiceImpl(List<ImageStorageHandler> handlers) {
		handlers.forEach(handler -> handlerMap.put(handler.getType(),handler));
	}

	/**
	 * 上传图片
	 * <p>
	 * 根据配置的存储类型路由到对应的存储处理器处理，
	 * 逐个文件调用处理器完成上传，保存路径作为目录前缀传递给处理器；
	 * 存储类型未配置或无对应处理器时抛出业务异常。
	 * </p>
	 *
	 * @param param 图片上传参数，包含上传的图片文件列表与保存路径
	 * @return 上传结果列表，每张图片对应一项，包含文件原始名称、对象存储路径、访问地址与文件大小
	 */
	@Override
	public List<ImageUploadResult> upload(ImageUploadParam param) {

		ImageStorageType storeType = ImageStorageType.get(imageStorageProperties.getStoreType());

		ImageStorageHandler handler = handlerMap.get(storeType);

		if(ObjectUtil.isNullEmpty(handler)) {
			throw new BusinessResultException("未知的图片存储类型：" + imageStorageProperties.getStoreType());
		}

		List<ImageUploadResult> results = new ArrayList<>(param.getFiles().size());

		param.getFiles().forEach(file -> results.add(handler.upload(file,param.getPath())));

		return results;

	}

}
