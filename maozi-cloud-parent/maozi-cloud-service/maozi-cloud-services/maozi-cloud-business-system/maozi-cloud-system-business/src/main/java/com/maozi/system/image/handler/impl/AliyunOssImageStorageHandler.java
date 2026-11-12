package com.maozi.system.image.handler.impl;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.system.image.enums.ImageStorageType;
import com.maozi.system.image.handler.ImageStorageHandler;
import com.maozi.system.image.properties.ImageStorageProperties;
import com.maozi.system.image.result.ImageUploadResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 图片存储处理器
 * <p>
 * 基于 OSS Java SDK V2 实现图片的简单上传（PutObject），
 * 客户端在应用启动时根据配置初始化，应用关闭时释放资源。
 * 上传的对象路径格式为 目录前缀/日期目录/UUID.扩展名（目录前缀取上传参数的保存路径，为空时默认 image），
 * 避免同名覆盖与顺序前缀。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Component
public class AliyunOssImageStorageHandler implements ImageStorageHandler {

	/** 图片存储配置属性 */
	@Resource
	private ImageStorageProperties imageStorageProperties;

	/** OSS 同步客户端，应用启动时初始化 */
	private OSSClient ossClient;

	/**
	 * 初始化 OSS 客户端
	 * <p>
	 * 根据配置的地域、访问域名与访问凭证创建客户端，
	 * 访问域名未配置时由 SDK 根据地域自动解析。
	 * </p>
	 */
	@PostConstruct
	public void init() {

		ImageStorageProperties.Aliyun aliyun = imageStorageProperties.getAliyun();

		OSSClientBuilder builder = OSSClient.newBuilder()
			.region(aliyun.getRegion());

		if(StringUtils.isNotEmpty(aliyun.getEndpoint())) {
			builder.endpoint(aliyun.getEndpoint());
		}

		ossClient = builder
			.credentialsProvider(new StaticCredentialsProvider(aliyun.getAccessKeyId(),aliyun.getAccessKeySecret()))
			.build();

	}

	/**
	 * 释放 OSS 客户端资源
	 */
	@PreDestroy
	public void destroy() {
		if(ossClient != null) {
			try {
				ossClient.close();
			} catch (Exception e) {
				// 应用关闭阶段，客户端资源释放失败时忽略
				log.warn("close oss client failed on shutdown", e);
			}
		}
	}

	/**
	 * 获取处理器支持的存储类型
	 *
	 * @return 阿里云 OSS 存储类型
	 */
	@Override
	public ImageStorageType getType() {
		return ImageStorageType.ALIYUN_OSS;
	}

	/**
	 * 上传图片到阿里云 OSS
	 * <p>
	 * 校验文件非空后，按生成的对象路径将文件流写入配置的 Bucket，
	 * 上传成功后返回文件原始名称、对象存储路径、访问地址与文件大小。
	 * </p>
	 *
	 * @param file 上传的图片文件
	 * @param path 保存路径（目录前缀）
	 * @return 上传结果，包含文件原始名称、对象存储路径、访问地址与文件大小
	 * @throws BusinessResultException 上传文件为空或上传过程发生异常时抛出
	 */
	@Override
	public ImageUploadResult upload(MultipartFile file,String path) {

		// 上传文件为空时抛出业务异常
		if(ObjectUtil.isNullEmpty(file) || file.isEmpty()) {
			throw new BusinessResultException(new ErrorCode("上传文件不能为空"));
		}

		try {

			ImageStorageProperties.Aliyun aliyun = imageStorageProperties.getAliyun();

			String fileKey = buildObjectKey(path,file.getOriginalFilename());

			PutObjectRequest request = PutObjectRequest.newBuilder()
				.bucket(aliyun.getBucket())
				.key(fileKey)
				.contentType(file.getContentType())
				.body(BinaryData.fromStream(file.getInputStream(),file.getSize()))
				.build();

			ossClient.putObject(request);

			// 对象存储路径统一补上前导斜杠后返回
			return new ImageUploadResult(file.getOriginalFilename(),"/" + fileKey,buildAccessUrl(fileKey), file.getSize());

		} catch (Exception e) {
			// 上传过程中的任意异常统一转换为业务异常抛出
			throw new BusinessResultException(e.getMessage(),new ErrorCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE,"文件上传失败"));
		}

	}

	/**
	 * 构建对象存储路径
	 * <p>
	 * 格式为 目录前缀/yyyy/MM/dd/UUID.扩展名，
	 * 目录前缀取入参传递的保存路径（接口层已校验非空，首尾斜杠会被清除，清除后为空时默认 image），
	 * 使用随机 UUID 避免同名文件覆盖（UUID 中的连字符会被去除），
	 * 扩展名统一转为小写（文件无扩展名时省略），日期目录便于按时间归类管理。
	 * </p>
	 *
	 * @param path 保存路径（目录前缀）
	 * @param originalFilename 文件原始名称
	 * @return 对象存储路径（Object Key）
	 */
	private String buildObjectKey(String path,String originalFilename) {

		String directory = StringUtils.defaultIfEmpty(StringUtils.strip(path,"/"),"image");

		String extension = StringUtils.substringAfterLast(originalFilename,".");

		String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

		String fileName = UUID.randomUUID().toString().replace("-","");

		return directory + "/" + datePath + "/" + fileName + (StringUtils.isNotEmpty(extension) ? "." + extension.toLowerCase() : "");

	}

	/**
	 * 构建文件访问地址
	 * <p>
	 * 配置了自定义访问域名时基于该域名生成；
	 * 否则基于访问域名（Endpoint）拼接 Bucket 子域名生成；
	 * 两者均未配置时按地域默认公网域名生成。
	 * </p>
	 *
	 * @param fileKey 对象存储路径（Object Key）
	 * @return 文件访问地址
	 */
	private String buildAccessUrl(String fileKey) {

		ImageStorageProperties.Aliyun aliyun = imageStorageProperties.getAliyun();

		if(StringUtils.isNotEmpty(aliyun.getDomain())) {
			return StringUtils.removeEnd(aliyun.getDomain(),"/") + "/" + fileKey;
		}

		if(StringUtils.isNotEmpty(aliyun.getEndpoint())) {

			URI endpoint = URI.create(aliyun.getEndpoint());

			return endpoint.getScheme() + "://" + aliyun.getBucket() + "." + endpoint.getHost() + "/" + fileKey;

		}

		return "https://" + aliyun.getBucket() + ".oss-" + aliyun.getRegion() + ".aliyuncs.com/" + fileKey;

	}

}
