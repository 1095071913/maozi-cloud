package com.maozi.service.api.rpc;

import com.maozi.base.param.SaveUpdateBatch;
import com.maozi.base.result.DropDownResult;
import com.maozi.common.result.AbstractBaseResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 基础服务结果接口
 * <p>
 * 定义通用的 CRUD 服务方法签名，由 {@code BaseServiceImpl} 统一实现，业务服务继承后即具备这些能力。
 * 提供单条查询、批量查询、关联查询、计数、新增/修改、删除、可用性校验和下拉列表等标准操作。
 * </p>
 *
 * @param <D> 数据传输对象类型
  * @author maozi
 * @date 2026/11/08
 */
public interface BaseServiceResult<D> {

    /**
     * 根据 ID 查询单条记录
     *
     * @param id 主键 ID
     * @param columns 查询字段列表
     * @return 查询结果
     */
    AbstractBaseResult<D> getByIdResult(Long id,String ... columns);

    /**
     * 根据 ID 集合批量查询记录
     *
     * @param ids 主键 ID 集合
     * @param columns 查询字段列表
     * @return 以 ID 为键、数据为值的映射
     */
    AbstractBaseResult<Map<Long,D>> listByIdsResult(Collection<Long> ids,String ... columns);

    /**
     * 根据关联 ID 查询关联记录列表
     *
     * @param id 关联 ID
     * @param relationField 关联字段名
     * @param columns 查询字段列表
     * @return 关联记录列表
     */
    AbstractBaseResult<List<D>> listByRelationIdResult(Long id,String relationField,String ... columns);

    /**
     * 根据关联 ID 集合批量查询关联记录
     *
     * @param ids 关联 ID 集合
     * @param relationField 关联字段名
     * @param columns 查询字段列表
     * @return 以关联 ID 为键、记录列表为值的映射
     */
    AbstractBaseResult<Map<Long,List<D>>> listByRelationIdsResult(Collection<Long> ids,String relationField,String ... columns);

    /**
     * 根据条件统计记录数
     *
     * @param dto 查询条件
     * @return 记录总数
     */
    AbstractBaseResult<Long> getCountByParamResult(D dto);

    /**
     * 新增或更新记录
     *
     * @param id 主键 ID（新增时为 null）
     * @param param 请求参数
     * @param <P> 参数类型
     * @return 操作后的记录 ID
     */
    <P> AbstractBaseResult<Long> saveUpdateResult(Long id,P param);

    /**
     * 批量新增或更新记录
     *
     * @param params 批量操作参数列表
     * @return 操作结果
     */
    AbstractBaseResult<Void> saveUpdateBatchResult(List<SaveUpdateBatch> params);

    /**
     * 根据 ID 删除记录
     *
     * @param id 主键 ID
     * @return 操作结果
     */
    AbstractBaseResult<Void> removeByIdResult(Long id);

    /**
     * 根据 ID 集合批量删除记录
     *
     * @param ids 主键 ID 集合
     * @return 操作结果
     */
    AbstractBaseResult<Void> removeByIdBatchResult(List<Long> ids);

    /**
     * 校验单条记录是否可用
     *
     * @param id 主键 ID
     * @return 校验结果
     */
    AbstractBaseResult<Void> checkAvailableResult(Long id);

    /**
     * 校验多条记录是否可用
     *
     * @param ids 主键 ID 集合
     * @return 校验结果
     */
    AbstractBaseResult<Void> checkAvailableResult(List<Long> ids);

    /**
     * 根据 ID 查询可用的单条记录
     *
     * @param id 主键 ID
     * @param columns 查询字段列表
     * @return 查询结果
     */
    AbstractBaseResult<D> getAvailableByIdResult(Long id,String ... columns);

    /**
     * 根据 ID 获取下拉选择项
     *
     * @param id 主键 ID
     * @return 下拉选择结果
     */
    AbstractBaseResult<DropDownResult> dropDownResult(Long id);

    /**
     * 根据 ID 集合批量获取下拉选择项
     *
     * @param ids 主键 ID 集合
     * @return 下拉选择列表
     */
    AbstractBaseResult<List<DropDownResult>> dropDownListResult(Collection<Long> ids);

}
