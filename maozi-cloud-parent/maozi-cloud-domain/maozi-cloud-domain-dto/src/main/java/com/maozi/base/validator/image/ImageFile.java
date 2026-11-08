package com.maozi.base.validator.image;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 图片文件校验注解
 * <p>
 * 标注在 {@link org.springframework.web.multipart.MultipartFile} 类型的字段上，
 * 或集合元素的类型位置（如 {@code List<@ImageFile MultipartFile>}），
 * 校验上传文件是否为图片格式（Content-Type 以 "image" 开头）。
 * </p>
 *
 * @author maozi
 */
@Target({ElementType.FIELD,ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ImageFileValidator.class)
public @interface ImageFile {

    /** 校验失败时的默认提示信息 */
    String message() default "非图片类型";

    /** 分组校验 */
    Class<?>[] groups() default {};

    /** 校验负载 */
    Class<? extends Payload>[] payload() default {};

}
