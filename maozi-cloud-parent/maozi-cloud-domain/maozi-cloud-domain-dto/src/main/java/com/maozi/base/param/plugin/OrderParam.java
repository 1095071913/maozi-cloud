package com.maozi.base.param.plugin;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 排序参数接口
 * <p>
 * 提供通用的排序功能定义，支持多表多字段排序。
 * 通过 {@link #getOrderFieldMap()} 获取前端传入的排序规则，
 * 并结合 {@link #getOrderMainFieldsMap()} 定义的主表可排序字段，
 * 自动将排序字段按升序/降序分组，转换为下划线命名的 SQL 排序字段。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public interface OrderParam {

	/** 排序字段 key：创建时间（构建排序条件时转换为下划线列名 create_time） */
	String CREATE_TIME_KEY = "createTime";

	/** 排序字段 key：更新时间（构建排序条件时转换为下划线列名 update_time） */
	String UPDATE_TIME_KEY = "updateTime";

	/**
	 * 获取排序字段映射
	 *
	 * @return key 为排序字段，value 为排序方向（true 降序、false 升序、null 不排序）
	 */
	@Schema(description = "排序组件 key:排序字段 value: true降序、false升序、null不做排序、空字符串字段")
	Map<String,Boolean> getOrderFieldMap();

	/**
	 * 获取主表可排序字段映射
	 *
	 * @return key 为主表别名，value 为该表下可排序的字段及默认排序方向
	 */
	@JsonIgnore
	Map<String,Map<String,Boolean>> getOrderMainFieldsMap();

	/**
	 * 获取升序排序字段映射
	 *
	 * @return key 为主表别名，value 为该表下需要升序排序的字段列表
	 */
	@JsonIgnore
	Map<String,List<String>> getOrderAscFieldsMap();

	/**
	 * 设置升序排序字段映射
	 *
	 * @param orderAscFieldsMap 升序排序字段映射
	 */
	@JsonIgnore
	void setOrderAscFieldsMap(Map<String,List<String>> orderAscFieldsMap);

	/**
	 * 获取降序排序字段映射
	 *
	 * @return key 为主表别名，value 为该表下需要降序排序的字段列表
	 */
	@JsonIgnore
	Map<String,List<String>> getOrderDescFieldsMap();

	/**
	 * 设置降序排序字段映射
	 *
	 * @param orderDescFieldsMap 降序排序字段映射
	 */
	@JsonIgnore
	void setOrderDescFieldsMap(Map<String,List<String>> orderDescFieldsMap);

	/**
	 * 初始化排序参数
	 * <p>
	 * 遍历主表可排序字段，结合前端传入的排序规则，将排序字段按升序/降序分组，
	 * 并转换为 "表别名.下划线字段名" 格式存储。
	 * </p>
	 */
	default void initOrderParam() {

		Map<String, Map<String, Boolean>> orderMainFieldsMap = getOrderMainFieldsMap();

		Map<String,List<String>> orderAscFieldsMap = CollectionUtil.newHashMap();

		Map<String,List<String>> orderDescFieldsMap = CollectionUtil.newHashMap();

		for(String key : orderMainFieldsMap.keySet()) {

			Map<String,Boolean> orderMainFields = orderMainFieldsMap.get(key);

			List<String> orderAscFields = CollectionUtil.newArrayList();

			List<String> orderDescFields = CollectionUtil.newArrayList();

			for(String orderMainField : orderMainFields.keySet()) {

				Map<String, Boolean> orderFieldMap = getOrderFieldMap();

				// 前端未传入排序规则时使用空 Map，避免空指针
				if(ObjectUtil.isNullEmpty(orderFieldMap)){
					orderFieldMap = CollectionUtil.newHashMap();
				}

				// 优先使用前端传入的排序方向，未指定时使用字段默认排序方向
				Boolean orderMainFieldValue = orderFieldMap.containsKey(orderMainField) ? orderFieldMap.get(orderMainField) : orderMainFields.get(orderMainField);

				if(ObjectUtil.isNotNullEmpty(orderMainFieldValue)) {

					String underlineCase = StrUtil.toUnderlineCase(key);

					// 表别名非空时拼接为 "表别名.字段名"，为空时仅取表别名的下划线形式
					String field = StringUtils.isEmpty(key) ? underlineCase : String.join(".", underlineCase, StrUtil.toUnderlineCase(orderMainField));

					if(orderMainFieldValue) {
						orderDescFields.add(field);
					}else {
						orderAscFields.add(field);
					}

				}

			}

			orderAscFieldsMap.put(key, orderAscFields);

			orderDescFieldsMap.put(key, orderDescFields);

		}

		setOrderAscFieldsMap(orderAscFieldsMap);

		setOrderDescFieldsMap(orderDescFieldsMap);

	}

}
