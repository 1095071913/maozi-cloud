package com.maozi.base.validator.video;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 视频文件校验注解
 * <p>
 * 标注在 {@link org.springframework.web.multipart.MultipartFile} 类型的字段上，
 * 校验上传文件是否为视频格式（Content-Type 以 "video/" 开头）。
 * </p>
 *
 * @author maozi
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VideoFileValidator.class)
public @interface VideoFile {

    /** 校验失败时的默认提示信息 */
    String message() default "非视频类型";

    /** 分组校验 */
    Class<?>[] groups() default {};

    /** 校验负载 */
    Class<? extends Payload>[] payload() default {};

}
