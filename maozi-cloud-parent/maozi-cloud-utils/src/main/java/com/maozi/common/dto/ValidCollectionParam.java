package com.maozi.common.dto;

import com.maozi.common.ValidatorUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

/**
 * 集合参数校验包装类
 * <p>
 * 用于将集合类型的参数包装为对象，以触发 Jakarta Validation 的级联校验。
 * 搭配 {@link ValidatorUtil} 使用。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidCollectionParam implements Serializable {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 待校验的集合数据 */
    @Valid
    private Collection<?> data;

}
