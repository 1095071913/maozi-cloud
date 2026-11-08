package com.maozi.system.image.param;

import com.maozi.base.validator.image.ImageFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 图片上传参数
 * <p>
 * 用于接收 multipart/form-data 形式提交的一个或多个图片文件
 * （表单字段名 files，重复该字段即可上传多张图片），
 * 通过 {@link ImageFile} 注解校验每个上传文件必须为图片格式，
 * 保存路径必填，作为对象存储的目录前缀。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadParam implements Serializable {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 上传的图片文件列表 */
	@Schema(description = "上传的图片文件列表")
	@NotEmpty(message = "上传文件不能为空")
	private List<@ImageFile MultipartFile> files;

	/** 保存路径，作为对象存储的目录前缀，如 avatar、product/banner */
	@Schema(description = "保存路径（对象存储目录前缀）",requiredMode = Schema.RequiredMode.REQUIRED)
	@NotEmpty(message = "保存路径不能为空")
	private String path;

}
