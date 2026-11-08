package com.maozi.base.validator.excel;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 文件校验注解
 * <p>
 * 标注在 {@link org.springframework.web.multipart.MultipartFile} 类型的字段上，
 * 校验上传文件是否为 Excel 格式（xls 或 xlsx）。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExcelFileValidator.class)
public @interface ExcelFile {

    /** 校验失败时的默认提示信息 */
    String message() default "非表格类型";

    /** 分组校验 */
    Class<?>[] groups() default {};

    /** 校验负载 */
    Class<? extends Payload>[] payload() default {};

}
