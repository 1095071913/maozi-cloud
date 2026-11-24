package com.maozi.common;

import com.maozi.common.dto.ValidCollectionParam;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 参数校验工具类
 * <p>
 * 封装 Jakarta Validation 的校验功能，支持单个对象和集合类型的参数校验。
 * 集合类型会自动包装为 {@link ValidCollectionParam} 后再进行校验。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public final class ValidatorUtil {

    /** 校验器（线程安全，全局单例） */
    private static final Validator VALIDATOR;

    /** 参数为空的提示信息 */
    private static final String PARAM_EMPTY_MESSAGE = "参数不能为空";

    /**
     * 工具类私有化构造，禁止实例化
     */
    private ValidatorUtil() {}

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    /**
     * 获取全局校验器
     *
     * @return 全局校验器
     */
    public static Validator getValidator() {
        return VALIDATOR;
    }

    /**
     * 统一校验方法：支持单个对象 / 集合自动包装校验
     *
     * @param data 待校验的数据，支持单个对象或集合
     * @throws BusinessResultException 参数为空或校验不通过时抛出
     */
    public static void validate(Object data) {

        if (Objects.isNull(data)) {
            throw new BusinessResultException(SystemErrorCode.PARAM_ERROR, PARAM_EMPTY_MESSAGE);
        }

        Object target = data;
        if (data instanceof Collection<?> collection) {
            target = ValidCollectionParam.builder().data(collection).build();
        }

        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(target);
        if (CollectionUtil.isEmpty(violations)) {
            return;
        }

        Map<String, String> errorMap = CollectionUtil.newHashMap(violations.size());
        for (ConstraintViolation<Object> v : violations) {
            errorMap.put(v.getPropertyPath().toString(), v.getMessage());
        }

        throw new BusinessResultException(SystemErrorCode.PARAM_ERROR, errorMap);

    }

}
