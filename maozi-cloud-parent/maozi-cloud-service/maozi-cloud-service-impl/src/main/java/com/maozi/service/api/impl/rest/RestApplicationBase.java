package com.maozi.service.api.impl.rest;

import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.success.SuccessResult;
import com.maozi.service.annotation.Get;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.result.TemplateResult;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 应用基础 REST 控制器
 * <p>
 * 提供系统级别的基础接口，当前仅有模版接口，用于在接口文档中展示通用返回结构，业务无需调用。
 * </p>
 *
 * @author maozi
 */
@RestService
@Tag(name = "公用模块/无用忽略掉")
public class RestApplicationBase {

    /** 基础路径 */
    private static final String PATH = "/application";

    /**
     * 模版接口
     *
     * @return 空的模版结果
     */
    @Get(value = PATH + "/template",description = "模版")
    public AbstractBaseResult<TemplateResult> template() {
        return new SuccessResult<>(null);
    }

}
