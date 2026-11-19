package com.maozi.base.validator.image;

import com.maozi.common.constant.SymbolConstant;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Set;

/**
 * 图片文件校验器
 * <p>
 * 实现 {@link ConstraintValidator} 接口，校验上传文件是否为图片格式。
 * 同时校验文件扩展名是否在白名单内以及 Content-Type 是否以 "image" 开头。
 * 文件为空时跳过校验。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class ImageFileValidator implements ConstraintValidator<ImageFile, MultipartFile> {

    /** 允许上传的图片扩展名集合 */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /** 图片 Content-Type 前缀 */
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image";

    /**
     * 初始化校验器
     *
     * @param constraintAnnotation 注解实例
     */
    @Override
    public void initialize(ImageFile constraintAnnotation) {}

    /**
     * 校验上传文件是否为图片格式
     *
     * @param file 上传的文件
     * @param context 校验上下文
     * @return 文件为空时返回 true（跳过校验），否则返回扩展名在白名单内且 Content-Type 以 "image" 开头
     */
    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {

        // 文件为空时跳过校验，交给 @NotNull 等注解处理非空逻辑
        if (Objects.isNull(file) || file.isEmpty()) {
            return true;
        }

        // getOriginalFilename 可能为 null，取出的小写扩展名可能为空，此时直接不通过
        String contentType = file.getContentType();
        String extension = StringUtils.lowerCase(StringUtils.substringAfterLast(file.getOriginalFilename(), SymbolConstant.DOT));
        if(StringUtils.isEmpty(extension)) {
            return false;
        }
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension)
                && StringUtils.isNotEmpty(contentType)
                && contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX);
    }

}
