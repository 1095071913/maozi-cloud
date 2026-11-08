package com.maozi.system.config.api.impl;

import cn.hutool.extra.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.base.enums.Status;
import com.maozi.common.ObjectUtil;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.config.api.ConfigService;
import com.maozi.system.config.domain.ConfigDo;
import com.maozi.system.config.mapper.ConfigMapper;
import com.maozi.system.config.param.ConfigSaveUpdateParam;
import com.maozi.system.config.result.ConfigDropDownResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 全局配置服务实现类
 * <p>
 * 继承自 BaseServiceImpl，提供全局配置（Config）相关的基础业务操作实现。
 * 基于 MyBatis-Plus 的 Mapper 层实现数据库访问操作。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Service
public class ConfigServiceImpl extends BaseServiceImpl<ConfigMapper,ConfigDo,Void> implements ConfigService {

	/** 资源名称，用于日志和异常信息中标识当前操作的资源类型 */
	private static final String RESOURCE_NAME = "配置";

	/**
	 * 获取资源名称
	 *
	 * @return 返回当前服务所管理的资源名称 "配置"
	 */
	@Override
	protected String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * 根据配置名称获取配置信息
	 * <p>
	 * 配置名称为空时抛出业务异常，配置不存在时返回 null。
	 * </p>
	 *
	 * @param name 配置名称
	 * @return 配置信息，包含配置 ID、名称、别名与配置值；配置不存在时返回 null
	 */
	@Override
	public ConfigDropDownResult getConfigByName(String name) {

		// 配置名称为空时抛出业务异常
		ObjectUtil.isNullEmptyThrowError(name, getResourceName());

		LambdaQueryWrapper<ConfigDo> wrapper = Wrappers.lambdaQuery();

		// 仅查询ID、名称、别名和配置值字段
		wrapper.select(ConfigDo::getId,ConfigDo::getName,ConfigDo::getAlias,ConfigDo::getValue);
		wrapper.eq(ConfigDo::getName,name);

		ConfigDo domain = getOne(wrapper);

		// 配置不存在时返回 null
		if(ObjectUtil.isNullEmpty(domain)){
			return null;
		}

		return CglibUtil.copy(domain, ConfigDropDownResult.class);

	}

	/**
	 * 根据配置类型获取启用状态的下拉列表数据
	 * <p>
	 * 配置类型为空时抛出业务异常，仅返回启用状态下指定类型的配置选项。
	 * </p>
	 *
	 * @param type 配置类型（必传）
	 * @return 下拉列表数据，每项包含配置 ID、名称、别名、配置值与排序
	 */
	protected List<ConfigDropDownResult> dropDownList(String type) {

		// 配置类型为空时抛出业务异常
		ObjectUtil.isNullEmptyThrowError(type, getResourceName() + "类型");

		LambdaQueryWrapper<ConfigDo> wrapper = Wrappers.lambdaQuery();

		// 仅查询ID、名称、别名、配置值和排序字段
		wrapper.select(ConfigDo::getId,ConfigDo::getName,ConfigDo::getAlias,ConfigDo::getValue,ConfigDo::getSort);

		// 根据配置类型筛选
		wrapper.eq(ConfigDo::getType,type);

		// 仅查询启用状态的配置
		wrapper.eq(ConfigDo::getStatus,Status.ENABLE);

		// 按排序值升序排列，排序值相同时按创建时间升序排列
		wrapper.orderByAsc(ConfigDo::getSort,ConfigDo::getCreateTime);

		return list(wrapper,ConfigDropDownResult::new);

	}

	/**
	 * 保存/更新配置（REST 层入口）
	 * <p>
	 * 新增时各字段必填（由基类触发参数校验），更新时仅更新传入的字段；
	 * 传入配置名称时校验其唯一性（更新时排除自身记录），未传入则跳过校验。
	 * </p>
	 *
	 * @param id    配置 ID（为空时执行新增）
	 * @param param 配置保存/更新参数
	 * @return 新增或更新后的配置 ID
	 */
	protected Long restSaveUpdate(Long id, ConfigSaveUpdateParam param) {

		// 传入配置名称时校验唯一性，更新时排除自身记录；未传入名称则无需校验
		if(ObjectUtil.isNotNullEmpty(param.getName())) {

			LambdaQueryWrapper<ConfigDo> wrapper = Wrappers.lambdaQuery();
			wrapper.eq(ConfigDo::getName,param.getName());

			if(ObjectUtil.isNotNullEmpty(id)) {
				wrapper.ne(ConfigDo::getId,id);
			}

			checkNotHas(wrapper);

		}

		return saveUpdate(id,param);

	}

}
