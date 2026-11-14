package com.maozi.common;

import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 集合工具类
 * <p>
 * 提供集合（List、Map、Set）的创建工厂方法和空值判断工具方法，
 * 以及集合为空时抛出异常的校验方法。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@SuppressWarnings("unchecked")
public class CollectionUtil {

    /**
     * 创建空的 ArrayList
     *
     * @param <T> 元素类型
     * @return 空的 ArrayList
     */
    public static <T> List<T> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * 根据可变参数创建 ArrayList
     *
     * @param array 元素数组
     * @param <T> 元素类型
     * @return 包含指定元素的 ArrayList
     */
    public static <T> List<T> newArrayList(T ... array) {
        ArrayList<T> list = new ArrayList<>(array.length);
        Collections.addAll(list, array);
        return list;
    }

    /**
     * 创建指定初始容量的 ArrayList
     *
     * @param initialCapacity 初始容量（预期元素个数）
     * @param <T> 元素类型
     * @return 指定初始容量的 ArrayList
     */
    public static <T> List<T> newArrayList(int initialCapacity) {
        return new ArrayList<>(initialCapacity);
    }

    /**
     * 创建空的 HashMap
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 空的 HashMap
     */
    public static <K,V> Map<K,V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * 创建指定预期元素个数的 HashMap
     * <p>
     * 按负载因子 0.75 换算实际初始容量，避免写入预期数量元素时触发扩容 rehash。
     * </p>
     *
     * @param expectedSize 预期元素个数
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 按预期元素个数定容的 HashMap
     */
    public static <K,V> Map<K,V> newHashMap(int expectedSize) {
        return new HashMap<>((int) (expectedSize / 0.75f) + 1);
    }

    /**
     * 创建 HashMap 并初始化放入第一个键值对
     *
     * @param key 首个键
     * @param value 首个值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 包含指定键值对的 HashMap
     */
    public static <K,V> Map<K,V> newHashMap(K key, V value) {
        HashMap<K, V> map = new HashMap<>((int) (1 / 0.75f) + 1);
        map.put(key, value);
        return map;
    }

    /**
     * 创建空的 HashSet
     *
     * @param <T> 元素类型
     * @return 空的 HashSet
     */
    public static <T> Set<T> newHashSet() {
        return new HashSet<>();
    }

    /**
     * 创建指定预期元素个数的 HashSet
     * <p>
     * 按负载因子 0.75 换算实际初始容量，避免写入预期数量元素时触发扩容 rehash。
     * </p>
     *
     * @param expectedSize 预期元素个数
     * @param <T> 元素类型
     * @return 按预期元素个数定容的 HashSet
     */
    public static <T> Set<T> newHashSet(int expectedSize) {
        return new HashSet<>((int) (expectedSize / 0.75f) + 1);
    }

    /**
     * 根据可变参数创建 HashSet
     *
     * @param array 元素数组
     * @param <T> 元素类型
     * @return 包含指定元素的 HashSet
     */
    public static <T> Set<T> newHashSet(T ... array) {
        return new HashSet<>(newArrayList(array));
    }

    /**
     * 根据已有 Map 创建新的 HashMap
     *
     * @param oldMap 原始 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 包含原始数据的新 HashMap
     */
    public static <K,V> Map<K,V> newHashMap(Map<K,V> oldMap) {
        return new HashMap<>(oldMap);
    }

    /**
     * 判断集合是否为空
     *
     * @param collection 集合
     * @return 为 null 或空时返回 true
     */
    public static boolean isEmpty(Collection<?> collection) {
        return Objects.isNull(collection) || collection.isEmpty();
    }

    /**
     * 判断集合是否不为空
     *
     * @param collection 集合
     * @return 不为 null 且不为空时返回 true
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断 Map 是否为空
     *
     * @param map Map 对象
     * @return 为 null 或空时返回 true
     */
    public static boolean isEmpty(Map<?,?> map) {
        return Objects.isNull(map) || map.isEmpty();
    }

    /**
     * 判断 Map 是否不为空
     *
     * @param map Map 对象
     * @return 不为 null 且不为空时返回 true
     */
    public static boolean isNotEmpty(Map<?,?> map) {
        return !isEmpty(map);
    }

    /**
     * 校验集合不为空，为空时抛出业务异常
     *
     * @param collection 集合
     * @param resourceName 资源名称（用于异常提示定位）
     * @param <T> 元素类型
     * @return 原集合
     * @throws BusinessResultException 集合为 null 或空时抛出
     */
    public static <T> Collection<T> collectionIsEmptyThrowError(Collection<T> collection,String resourceName) {

        if(ObjectUtil.isNullEmpty(collection)) {
            throw new BusinessResultException(SystemErrorCode.DATA_NOT_EXIST_ERROR)
                    .setResource(resourceName);
        }

        return collection;

    }

    /**
     * 校验 Map 不为空，为空时抛出业务异常
     *
     * @param map Map 对象
     * @param resourceName 资源名称（用于异常提示定位）
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 原 Map
     * @throws BusinessResultException Map 为 null 或空时抛出
     */
    public static <K,V> Map<K,V> collectionIsEmptyThrowError(Map<K,V> map,String resourceName) {

        if(ObjectUtil.isNullEmpty(map)) {
            throw new BusinessResultException(SystemErrorCode.DATA_NOT_EXIST_ERROR)
                    .setResource(resourceName);
        }

        return map;

    }

}
