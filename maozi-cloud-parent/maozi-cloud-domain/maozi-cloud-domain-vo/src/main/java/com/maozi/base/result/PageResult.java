package com.maozi.base.result;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果封装类
 * <p>
 * 通用的分页数据返回结构，包含当前页码、每页数量、数据总条数和当前页数据列表，
 * 使用泛型适配不同的业务数据类型；全参构造方法参数顺序为 current、size、total、data。
 * </p>
 *
 * @param <D> 当前页数据的类型，通常为业务 VO 类
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<D> implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 当前页码，从1开始计数 */
	@Schema(description = "页数")
	private Long current;

	/** 每页显示的数据条数 */
	@Schema(description = "每页数量")
	private Long size;

	/** 符合查询条件的数据总条数，用于前端计算总页数 */
	@Schema(description = "数据总数")
	private Long total;

	/** 当前页的数据列表 */
	@Valid
	private List<D> data;

}
