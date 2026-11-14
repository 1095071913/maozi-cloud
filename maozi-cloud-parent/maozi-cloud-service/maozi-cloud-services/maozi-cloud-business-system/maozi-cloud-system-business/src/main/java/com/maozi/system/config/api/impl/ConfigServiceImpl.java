package com.maozi.system.config.api.impl;

import cn.hutool.extra.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.base.enums.Status;
import com.maozi.common.ObjectUtil;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.config.api.ConfigService;
import com.maozi.system.config.domain.ConfigDO;
import com.maozi.system.config.mapper.ConfigMapper;
import com.maozi.system.config.param.ConfigSaveUpdateParam;
import com.maozi.system.config.result.ConfigDropDownResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 全局配置服务实现类
 * <p>
 * 继承 BaseServiceImpl，提供全局配置的基础业务操作实现。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class ConfigServiceImpl extends BaseServiceImpl<ConfigMapper, ConfigDO> implements ConfigService {

	/** 资源名称，用于日志和异常信息中标识当前操作的资源类型 */
	private static final String RESOURCE_NAME = "配置";

	/**
	 * 获取资源名称
	 */
	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * 根据配置名称获取配置信息
	 * <p>
	 * 配置名称为空时抛出业务异常。
	 * </p>
	 *
	 * @param name 配置名称
	 * @return 配置信息，包含配置 ID、名称、别名与配置值；配置不存在时返回 null
	 */
	@Override
	public ConfigDropDownResult commonGetConfigByName(String name) {

		// 配置名称为空时抛出业务异常
		ObjectUtil.isNullEmptyThrowError(name, getResourceName());

		LambdaQueryWrapper<ConfigDO> wrapper = Wrappers.lambdaQuery();

		// 仅查询ID、名称、别名和配置值字段
		wrapper.select(ConfigDO::getId, ConfigDO::getName, ConfigDO::getAlias, ConfigDO::getValue);
		wrapper.eq(ConfigDO::getName, name);

		ConfigDO domain = super.getOne(wrapper);

		if(ObjectUtil.isNullEmpty(domain)){
			return null;
		}

		return CglibUtil.copy(domain, ConfigDropDownResult.class);

	}

	/**
	 * 根据配置类型获取启用状态的下拉列表数据
	 * <p>
	 * 配置类型为空时抛出业务异常。
	 * </p>
	 *
	 * @param type 配置类型
	 * @return 下拉列表数据，每项包含配置 ID、名称、别名、配置值与排序
	 */
	public List<ConfigDropDownResult> commonDropDownList(String type) {

		// 配置类型为空时抛出业务异常
		ObjectUtil.isNullEmptyThrowError(type, getResourceName() + "类型");

		LambdaQueryWrapper<ConfigDO> wrapper = Wrappers.lambdaQuery();

		// 仅查询ID、名称、别名、配置值和排序字段
		wrapper.select(ConfigDO::getId, ConfigDO::getName, ConfigDO::getAlias, ConfigDO::getValue, ConfigDO::getSort);

		// 根据配置类型筛选启用状态的配置
		wrapper.eq(ConfigDO::getType, type);

		wrapper.eq(ConfigDO::getStatus, Status.ENABLE);

		wrapper.orderByAsc(ConfigDO::getSort, ConfigDO::getCreateTime);

		return super.list(wrapper, ConfigDropDownResult::new);

	}

	/**
	 * 保存/更新配置
	 * <p>
	 * 传入配置名称时校验其唯一性，更新时排除自身记录；新增或更新的校验由基类 saveUpdate 完成。
	 * </p>
	 *
	 * @param id    配置 ID，为空时执行新增
	 * @param param 配置保存/更新参数
	 * @return 新增或更新后的配置 ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long commonSaveUpdate(Long id, ConfigSaveUpdateParam param) {

		// 传入配置名称时校验唯一性，更新时排除自身记录
		if(ObjectUtil.isNotNullEmpty(param.getName())) {

			LambdaQueryWrapper<ConfigDO> wrapper = Wrappers.lambdaQuery();
			wrapper.eq(ConfigDO::getName, param.getName());

			if(ObjectUtil.isNotNullEmpty(id)) {
				wrapper.ne(ConfigDO::getId, id);
			}

			super.checkNotHas(wrapper);

		}

		return super.saveUpdate(id, param);

	}

}
