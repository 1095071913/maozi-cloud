package com.maozi.service.api.impl.result;

import cn.hutool.extra.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.maozi.base.enums.Status;
import com.maozi.base.param.RequestParam;
import com.maozi.base.param.SaveUpdateBatch;
import com.maozi.base.result.DropDownResult;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.ReflectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.db.domain.AbstractBaseDomain;
import com.maozi.db.domain.AbstractBaseNameDomain;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.service.api.rpc.BaseServiceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 业务结果封装基础实现
 * <p>
 * 在 {@link BaseServiceImpl} 通用增删改查能力之上，负责实体到 DTO 的属性拷贝，
 * 并将业务数据包装为统一响应结果（{@link AbstractBaseResult}）。
 * </p>
 *
 * @param <S> 关联的通用业务服务类型
 * @param <D> 结果 DTO 类型
 * @author pengjinlong
 * @date 2026/11/11 21:41
 */
@SuppressWarnings("unchecked")
public class BaseServiceResultImpl<S extends BaseServiceImpl, D> implements BaseServiceResult<D> {

    /** 关联的通用业务服务 */
    @Autowired
    protected S service;

    /** 数据传输对象（DTO）Class 对象 */
    protected Class<D> resultClass;

    /** 资源名称后缀：列表 */
    private static final String RESOURCE_SUFFIX_LIST = "列表";

    /** 资源名称后缀：分组 */
    private static final String RESOURCE_SUFFIX_GROUP = "分组";

    /**
     * 构造方法
     * <p>
     * 通过反射获取父类泛型参数的实际类型，初始化 resultClass。
     * </p>
     */
    public BaseServiceResultImpl() {

        // 沿继承链向上查找带泛型参数的父类，提取第二个泛型实参作为结果 DTO 类型
        for (Class<?> superClass = this.getClass();
             superClass != null && superClass != Object.class;
             superClass = superClass.getSuperclass()) {

            Type genericSuperclass = superClass.getGenericSuperclass();

            if(genericSuperclass instanceof ParameterizedType type) {

                resultClass = (Class<D>) type.getActualTypeArguments()[1];

                break;

            }

        }

    }

    /**
     * 获取结果 DTO 的 Class 对象
     * <p>
     * 如果 resultClass 为 Void 类型，说明未设置响应类型，抛出系统错误。
     * </p>
     *
     * @return 结果 DTO 的 Class 对象
     * @throws BusinessResultException 当未设置响应类型时抛出异常
     */
    public Class<D> getResultClass(){

        if(Void.class.equals(resultClass)) {
            throw new BusinessResultException(SystemErrorCode.NOT_SET_RESPONSE_ERROR)
                    .setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);
        }

        return resultClass;

    }

    /**
     * 批量获取数据库列名
     *
     * @param <O> 实体类型
     * @param functions Lambda 函数引用数组
     * @return 数据库列名数组
     */
    public <O> String[] getColumns(SFunction<O, ?>... functions) {
        return service.getColumns(functions);
    }

    /**
     * 根据 Lambda 函数引用获取对应的数据库列名
     *
     * @param <O> 实体类型
     * @param sfunction Lambda 函数引用
     * @return 数据库列名
     */
    public <O> String getColumn(SFunction<O, ?> sfunction) {
        return service.getColumn(sfunction);
    }

    /**
     * 根据 Class 对象获取其对应的所有数据库列名（默认为关联查询模式）
     *
     * @param clazz Class 对象
     * @return 数据库列名数组
     */
    public String[] getColumns(Class<?> clazz){
        return service.getColumns(clazz,true);
    }

    /**
     * 根据 Class 对象获取其对应的所有数据库列名
     *
     * @param clazz Class 对象
     * @param isJoin 是否为关联查询模式（关联查询时列名会添加表别名前缀）
     * @return 数据库列名数组
     */
    public String[] getColumns(Class<?> clazz,Boolean isJoin) {
        return service.getColumns(clazz, isJoin);
    }

    /**
     * 根据 ID 查询实体并封装为 DTO 结果
     *
     * @param id 实体 ID
     * @param columns 查询字段列表
     * @return 包含 DTO 的统一响应结果
     */
    @Override
    public AbstractBaseResult<D> getByIdResult(Long id, String ... columns) {
        AbstractBaseDomain domain = service.getById(id, columns);
        return ResultUtil.success(CglibUtil.copy(domain, getResultClass()));
    }

    /**
     * 根据 ID 集合批量查询并封装为以 ID 为键的 Map 结果
     *
     * @param ids 实体 ID 集合
     * @param columns 查询字段列表
     * @return 包含 Map&lt;Long, D&gt; 的统一响应结果
     */
    @Override
    public AbstractBaseResult<Map<Long,D>> listByIdsResult(Collection<Long> ids, String ... columns) {

        List<AbstractBaseDomain> domains = service.listByIds(ids, columns);

        List<D> responseItems = CglibUtil.copyList(domains, () -> ReflectUtil.newInstance(getResultClass()));

        return ResultUtil.success(

                responseItems.stream().collect(

                        Collectors.toMap(
                                ReflectUtil::invokeFunGetId
                                ,
                                Function.identity()
                        )

                )
        );

    }

    /**
     * 根据单个关联 ID 查询关联列表并封装为结果
     *
     * @param id 关联 ID
     * @param relationField 关联字段名
     * @param columns 查询字段列表
     * @return 包含 List&lt;D&gt; 的统一响应结果
     */
    @Override
    public AbstractBaseResult<List<D>> listByRelationIdResult(Long id,String relationField,String ... columns) {

        ObjectUtil.isNullEmptyThrowError(id, service.getResourceName());

        ObjectUtil.isNullEmptyThrowError(relationField, service.getResourceName() + RESOURCE_SUFFIX_GROUP);

        QueryWrapper<AbstractBaseDomain> wrapper = Wrappers.query();
        if(columns.length > 0) {
            wrapper.select(columns);
        }
        wrapper.eq(relationField,id);

        return ResultUtil.success(CglibUtil.copyList(service.list(wrapper), () -> ReflectUtil.newInstance(getResultClass())));

    }

    /**
     * 根据关联字段分组查询并封装为 Map 结果
     *
     * @param ids 关联 ID 集合
     * @param relationField 关联字段名
     * @param columns 查询字段列表
     * @return 包含 Map&lt;Long, List&lt;D&gt;&gt; 的统一响应结果
     */
    @Override
    public AbstractBaseResult<Map<Long, List<D>>> listByRelationIdsResult(
            Collection<Long> ids, String relationField, String ... columns) {

        CollectionUtil.collectionIsEmptyThrowError(ids,service.getResourceName() + RESOURCE_SUFFIX_LIST);

        ObjectUtil.isNullEmptyThrowError(relationField, service.getResourceName() + RESOURCE_SUFFIX_GROUP);

        QueryWrapper<AbstractBaseDomain> wrapper = Wrappers.query();
        if(columns.length > 0) {
            wrapper.select(columns);
        }
        wrapper.in(relationField,ids);

        List<AbstractBaseDomain> domains = service.list(wrapper);
        if(ObjectUtil.isNullEmpty(domains)) {
            return ResultUtil.success(CollectionUtil.newHashMap());
        }

        List<D> responseItems = CglibUtil.copyList(domains, () -> ReflectUtil.newInstance(getResultClass()));
        return ResultUtil.success(

                responseItems.stream().collect(
                        Collectors.groupingBy((resultData) ->
                                ReflectUtil.invokeGet(resultData, relationField), Collectors.toList()
                        )
                )

        );

    }

    /**
     * 根据参数统计记录数并封装为结果
     *
     * @param dto 查询参数
     * @return 包含记录数的统一响应结果
     */
    @Override
    public AbstractBaseResult<Long> getCountByParamResult(D dto){
        return ResultUtil.success(service.getCountByParam(dto));
    }

    /**
     * 新增或更新实体并封装为结果
     *
     * @param <P> 参数类型
     * @param id 实体 ID（为空时执行新增）
     * @param param 参数对象
     * @return 包含实体 ID 的统一响应结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public <P> AbstractBaseResult<Long> saveUpdateResult(Long id, P param){
        return ResultUtil.success(service.saveUpdate(id, param));
    }

    /**
     * 批量新增或更新并封装为结果
     *
     * @param collection 新增/更新参数列表
     * @return 统一响应结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AbstractBaseResult<Void> saveUpdateBatchResult(List<SaveUpdateBatch> collection){

        CollectionUtil.collectionIsEmptyThrowError(collection, service.getResourceName() + RESOURCE_SUFFIX_LIST);

        List<?> domains = CglibUtil.copyList(collection, () -> ReflectUtil.newInstance(service.getDomainClass()));
        service.saveUpdateBatch(domains);

        return ResultUtil.success();

    }



    /**
     * 更新实体状态
     * <p>
     * 目标状态为禁用（DISABLE）时先检查绑定关系；
     * 随后通过 UpdateWrapper 按 ID 仅更新状态字段。
     * </p>
     *
     * @param id 实体 ID
     * @param param 携带目标状态的请求参数（data 为空时抛出参数错误异常）
     * @return 统一响应结果
     */
    public AbstractBaseResult<Void> updateStatusResult(Long id, RequestParam<Status> param){

        Status status = param.getData();
        ObjectUtil.isNullEmptyThrowError(status, SystemErrorCode.PARAM_ERROR, "状态");

        // 目标状态为禁用时，需先检查是否存在绑定关系
        if(param.getData() == Status.DISABLE){
            service.checkBind(id);
        }

        LambdaUpdateWrapper<? extends AbstractBaseDomain> updateWrapper = Wrappers.lambdaUpdate(service.getDomainClass());
        updateWrapper.set(AbstractBaseDomain::getStatus, status);
        updateWrapper.eq(AbstractBaseDomain::getId, id);

        service.update(updateWrapper);

        return ResultUtil.success();

    }

    /**
     * 根据 ID 删除实体并封装为结果
     *
     * @param id 实体 ID
     * @return 统一响应结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AbstractBaseResult<Void> removeByIdResult(Long id) {
        service.removeById(id);
        return ResultUtil.success();
    }

    /**
     * 批量删除实体并封装为结果
     *
     * @param ids 实体 ID 列表
     * @return 统一响应结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AbstractBaseResult<Void> removeByIdBatchResult(List<Long> ids) {

        CollectionUtil.collectionIsEmptyThrowError(ids, service.getResourceName() + RESOURCE_SUFFIX_LIST);

        ids.forEach(service::removeById);

        return ResultUtil.success();

    }

    /**
     * 检查单个实体是否可用并封装为结果
     *
     * @param id 实体 ID
     * @return 统一响应结果
     */
    @Override
    public AbstractBaseResult<Void> checkAvailableResult(Long id){
        service.checkAvailable(id);
        return ResultUtil.success();
    }

    /**
     * 批量检查实体是否可用并封装为结果
     *
     * @param ids 实体 ID 列表
     * @return 统一响应结果
     */
    @Override
    public AbstractBaseResult<Void> checkAvailableResult(List<Long> ids){
        service.checkAvailable(ids);
        return ResultUtil.success();
    }

    /**
     * 根据 ID 查询可用实体并封装为结果
     *
     * @param id 实体 ID
     * @param columns 查询字段列表
     * @return 包含 DTO 的统一响应结果
     */
    @Override
    public AbstractBaseResult<D> getAvailableByIdResult(Long id,String ... columns){

        AbstractBaseDomain domain = service.getAvailableById(id, columns);

        return ResultUtil.success(CglibUtil.copy(domain, getResultClass()));

    }

    /**
     * 根据 ID 获取下拉选项并封装为结果
     *
     * @param id 实体 ID
     * @return 包含下拉选项的统一响应结果
     */
    @Override
    public AbstractBaseResult<DropDownResult> dropDownResult(Long id){
        return ResultUtil.success(service.dropDown(id));
    }

    /**
     * 根据 ID 集合批量获取下拉选项并封装为结果
     *
     * @param ids 实体 ID 集合
     * @return 包含下拉选项列表的统一响应结果
     */
    @Override
    public AbstractBaseResult<List<DropDownResult>> dropDownListResult(Collection<Long> ids){

        QueryWrapper<AbstractBaseDomain> wrapper = Wrappers.query();

        wrapper.select(getColumns(AbstractBaseNameDomain::getId,AbstractBaseNameDomain::getName));

        wrapper.in(getColumn(AbstractBaseDomain::getId), ids);

        return ResultUtil.success(CglibUtil.copyList(service.list(wrapper), DropDownResult::new));

    }

    /**
     * 获取所有启用状态的下拉选项列表并封装为结果
     *
     * @return 包含下拉选项列表的统一响应结果
     */
    @Override
    public AbstractBaseResult<List<DropDownResult>> dropDownListResult(){
        return ResultUtil.success(service.dropDownList());
    }

}