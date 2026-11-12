package com.maozi.base.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;

/**
 * 字符串列表类型
 * <p>
 * 继承 {@link ArrayList}{@code <String>}，供 JSON 反序列化时正确推断元素类型，
 * 解决泛型擦除导致的类型丢失问题。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StringArrayList extends ArrayList<String> {

}
