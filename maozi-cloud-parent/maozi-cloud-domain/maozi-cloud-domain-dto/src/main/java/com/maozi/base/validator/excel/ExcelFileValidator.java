package com.maozi.base.validator.excel;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel 文件校验器
 * <p>
 * 实现 {@link ConstraintValidator} 接口，校验上传文件是否为 Excel 格式。
 * 支持的 Content-Type：{@code application/vnd.ms-excel}（xls）
 * 和 {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}（xlsx）。
 * 文件为空时跳过校验。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class ExcelFileValidator implements ConstraintValidator<ExcelFile, MultipartFile> {

    /** xls 格式的 Content-Type */
    private static final String CONTENT_TYPE_XLS = "application/vnd.ms-excel";

    /** xlsx 格式的 Content-Type */
    private static final String CONTENT_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 初始化校验器
     *
     * @param constraintAnnotation 注解实例
     */
    @Override
    public void initialize(ExcelFile constraintAnnotation) {}

    /**
     * 校验上传文件是否为 Excel 格式
     *
     * @param file 上传的文件
     * @param context 校验上下文
     * @return 文件为空时返回 true（跳过校验），否则返回 Content-Type 是否匹配 Excel 格式
     */
    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {

        // 文件为空时跳过校验，交给 @NotNull 等注解处理非空逻辑
        if (file == null || file.isEmpty()) {
            return true;
        }

        // 仅接受 xls 与 xlsx 两种 Excel 格式的 Content-Type
        String contentType = file.getContentType();
        return CONTENT_TYPE_XLS.equals(contentType) || CONTENT_TYPE_XLSX.equals(contentType);

    }

}
