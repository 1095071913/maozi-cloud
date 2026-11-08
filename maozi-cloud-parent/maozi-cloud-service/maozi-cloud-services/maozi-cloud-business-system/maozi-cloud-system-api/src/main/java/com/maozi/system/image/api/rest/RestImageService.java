package com.maozi.system.image.api.rest;

import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.annotation.Post;
import com.maozi.system.image.param.ImageUploadParam;
import com.maozi.system.image.result.ImageUploadResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;

import java.util.List;

/**
 * 图片模块 REST 接口
 * <p>
 * 提供图片上传的 RESTful API 接口定义，
 * 支持单次上传一个或多个图片文件，
 * 上传的图片根据配置路由到对应的存储处理器
 * （当前支持阿里云 OSS，通过 oss.store-type 配置）。
 * </p>
 *
 * @author maozi
 */
@Tag(name = "文件存储模块")
public interface RestImageService {

	/** 基础路径常量，图片模块的统一请求路径前缀 */
	String PATH = "/image";

	/**
	 * 上传图片
	 * <p>
	 * 以 multipart/form-data 形式上传一个或多个图片文件
	 * （表单字段名 files，重复该字段即可上传多张图片），
	 * 必填传递保存路径（表单字段名 path）作为对象存储的目录前缀，
	 * 校验通过后根据配置的存储类型路由到对应的处理器完成上传。
	 * </p>
	 *
	 * @param param 图片上传参数，包含上传的图片文件列表与保存路径（目录前缀，必填）
	 * @return 上传结果列表，每张图片对应一项，包含文件原始名称、对象存储路径、访问地址与文件大小
	 */
	@Post(value = PATH + "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE, description = "图片上传")
	AbstractBaseResult<List<ImageUploadResult>> restUpload(@Valid ImageUploadParam param);

}
