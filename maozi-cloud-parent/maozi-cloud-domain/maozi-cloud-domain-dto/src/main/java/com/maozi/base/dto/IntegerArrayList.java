package com.maozi.base.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;

/**
 * 整型列表类型
 * <p>
 * 继承 {@link ArrayList}{@code <Integer>}，供 JSON 反序列化时正确推断元素类型，
 * 解决泛型擦除导致的类型丢失问题。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/5/11 15:00
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IntegerArrayList extends ArrayList<Integer> {

}
