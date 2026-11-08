package com.maozi.system.image.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片上传结果
 * <p>
 * 用于图片上传接口的返回结果对象，
 * 包含原始文件名、对象存储路径以及文件访问地址等信息。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResult implements Serializable {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 文件原始名称 */
	@Schema(description = "文件原始名称")
	private String fileName;

	/** 对象存储文件路径（Object Key） */
	@Schema(description = "对象存储文件路径")
	private String fileKey;

	/** 文件访问地址 */
	@Schema(description = "文件访问地址")
	private String url;

	/** 文件大小（字节） */
	@Schema(description = "文件大小（字节）")
	private Long fileSize;

}
