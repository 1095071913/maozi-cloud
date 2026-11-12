package com.maozi.service.api.impl.rest;

import com.maozi.common.ResultUtil;
import com.maozi.common.enums.BaseEnum;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.mvc.config.enums.EnumConfig;
import com.maozi.service.annotation.Get;
import com.maozi.service.api.annotation.RestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * 枚举常量 REST 控制器
 * <p>
 * 提供枚举常量的查询接口，支持获取全部枚举列表和按名称查询单个枚举。
 * 供前端下拉选择等场景使用。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
@Tag(name = "公用模块/枚举常量")
public class RestApplicationCode {

    /** 基础路径 */
    private static final String PATH = "/application/enum";

    /**
     * 获取全部枚举列表
     *
     * @return 枚举名称到枚举常量列表的映射
     */
    @Get(value = PATH + "/list",description = "列表")
    public AbstractBaseResult<Map<String, List<BaseEnum>>> list() {
        return ResultUtil.success(EnumConfig.getEnums());
    }

    /**
     * 根据枚举名称获取枚举常量列表
     *
     * @param name 枚举名称（首字母小写）
     * @return 枚举常量列表
     */
    @Get(value = PATH + "/{name}/get",description = "详情")
    public AbstractBaseResult<List<BaseEnum>> get(@PathVariable("name") String name) {
        return ResultUtil.success(EnumConfig.getEnums().get(name));
    }

}
