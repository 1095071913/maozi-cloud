package com.maozi.service.api.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.cglib.CglibUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.base.MPJBaseService;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.constant.ResultFunName;
import com.maozi.base.enums.Status;
import com.maozi.base.enums.StoreClassType;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.param.SaveUpdateBatch;
import com.maozi.base.param.plugin.OrderParam;
import com.maozi.base.param.plugin.TimeParam;
import com.maozi.base.plugin.StoreClass;
import com.maozi.base.plugin.context.QueryEnvironmentContext;
import com.maozi.base.plugin.join.JoinBaseType;
import com.maozi.base.plugin.join.JoinPlugins;
import com.maozi.base.plugin.mapping.QueryMapping;
import com.maozi.base.plugin.query.QueryBaseType;
import com.maozi.base.plugin.query.QueryPlugin;
import com.maozi.base.plugin.type.JoinType;
import com.maozi.base.plugin.type.QueryType;
import com.maozi.base.result.DropDownResult;
import com.maozi.base.result.PageResult;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.ReflectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.SerializeUtil;
import com.maozi.common.ValidatorUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.db.domain.AbstractBaseDomain;
import com.maozi.db.domain.AbstractBaseNameDomain;
import com.maozi.service.api.IBaseMapper;
import com.maozi.service.api.rpc.BaseServiceResult;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 基础服务实现类
 * <p>
 * 提供通用的数据库 CRUD 操作封装，包括单条查询、列表查询、分页查询、
 * 下拉数据查询、新增/更新/删除操作，以及结果集封装方法。
 * 支持基于注解的动态查询条件构建（QueryPlugin）和关联查询（JoinPlugins），
 * 并通过 QueryMapping 注解实现关联数据的自动填充。
 * </p>
 * <p>
 * 泛型参数说明：
 * <ul>
 *   <li>M - MyBatis-Plus Mapper 接口类型</li>
 *   <li>T - 领域模型（数据库实体）类型</li>
 *   <li>D - 数据传输对象（DTO）类型</li>
 * </ul>
 * </p>
 *
 * @author maozi
 */
public abstract class BaseServiceImpl<M extends IBaseMapper<T>, T extends AbstractBaseDomain,D> extends ServiceImpl<M, T> implements MPJBaseService<T>,BaseServiceResult<D> {

	/** 领域模型（数据库实体）Class 对象 */
	protected Class<T> domainClass;

	/** 数据传输对象（DTO）Class 对象 */
	protected Class<D> resultClass;

	/**
	 * 获取资源名称（供子类实现）
	 * <p>
	 * 用于在异常信息中标识当前操作的资源名称，便于问题定位。
	 * </p>
	 *
	 * @return 资源名称
	 */
	protected abstract String getResourceName();

	/**
	 * 构造方法
	 * <p>
	 * 通过反射获取父类泛型参数的实际类型，初始化 domainClass 和 resultClass。
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	public BaseServiceImpl() {

		// 沿继承链向上查找带泛型参数的父类，提取领域模型与 DTO 的泛型实参
		for(Class<?> superClass = this.getClass() ;; superClass = superClass.getSuperclass()) {

		Type genericSuperclass = superClass.getGenericSuperclass();

			if(genericSuperclass instanceof ParameterizedType type) {

				domainClass = (Class<T>) type.getActualTypeArguments()[1];

				resultClass = (Class<D>) type.getActualTypeArguments()[2];

				break;

			}

		}

	}

    /**
     * 获取当前领域模型的 Class 对象
     *
     * @return 领域模型 Class 对象
     */
    @Override
    public Class<T> currentModelClass() {
        return super.currentModelClass();
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

		if(resultClass.getName().equals(Void.class.getName())) {
			throw new BusinessResultException(SystemErrorCode.NOT_SET_RESPONSE_ERROR).setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);
		}

    	return resultClass;

    }

    /**
     * 将分页参数转换为 MyBatis-Plus 分页对象
     *
     * @param <R> 分页数据类型
     * @param pageParam 分页查询参数
     * @return MyBatis-Plus 分页对象
     */
    protected <R> com.baomidou.mybatisplus.extension.plugins.pagination.Page<R> convertPage(PageParam<?> pageParam){
    	return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(),pageParam.getSize());
    }

    /**
     * 将 MyBatis-Plus 分页结果转换为自定义分页结果，并通过 Cglib 拷贝数据
     *
     * @param <R> 响应数据类型
     * @param page MyBatis-Plus 分页对象
     * @param target 目标类型的 Supplier
     * @return 自定义分页结果
     */
    protected <R> PageResult<R> convertPageResult(Page<T> page,Supplier<R> target) {
		return new PageResult<>(page.getCurrent(),page.getSize(),page.getTotal(),CglibUtil.copyList(page.getRecords(),target));
	}

    /**
     * 将 MyBatis-Plus 分页结果转换为自定义分页结果（直接使用已有数据列表）
     *
     * @param <R> 响应数据类型
     * @param page MyBatis-Plus 分页对象
     * @param responseData 响应数据列表
     * @return 自定义分页结果
     */
    protected <R> PageResult<R> convertPageResult(Page<T> page,List<R> responseData) {
		return new PageResult<>(page.getCurrent(),page.getSize(),page.getTotal(),responseData);
	}

    /**
     * 将 MyBatis-Plus 分页结果转换为自定义分页结果（类型一致时直接使用）
     *
     * @param <R> 响应数据类型
     * @param page MyBatis-Plus 分页对象
     * @return 自定义分页结果
     */
	protected <R> PageResult<R> convertPageResult(Page<R> page) {
		return new PageResult<>(page.getCurrent(),page.getSize(),page.getTotal(),page.getRecords());
	}

    /**
     * 将集合转换为以 ID 为键的 Map
     *
     * @param <R> 集合元素类型
     * @param collection 数据集合
     * @param function 提取 ID 的函数
     * @return 以 ID 为键、元素为值的 Map
     */
    protected <R> Map<Long,R> toMapByIds(List<R> collection,Function<R, Long> function) {
    	return collection.stream().collect(Collectors.toMap(function, Function.identity()));
	}

    /**
     * 根据 Lambda 函数引用获取对应的数据库列名
     * <p>
     * 通过序列化 Lambda 表达式获取属性名，再转换为下划线格式的数据库列名。
     * 优先从 MyBatis-Plus 的 TableInfo 中获取映射关系。
     * </p>
     *
     * @param <O> 实体类型
     * @param sfunction Lambda 函数引用
     * @return 数据库列名
     */
	@SneakyThrows
    protected <O> String getColumn(SFunction<O, ?> sfunction) {

		// 通过 writeReplace 方法反射获取 Lambda 的序列化元信息
		Method method = sfunction.getClass().getDeclaredMethod(SerializeUtil.WRITE_REPLACE_FIELD_NAME);

		method.setAccessible(true);

		SerializedLambda serializedLambda = (SerializedLambda) method.invoke(sfunction);

		String fieldWithGet = serializedLambda.getImplMethodName();

		// 去掉方法名的 get 前缀得到属性名，并转为下划线格式
		String propertyName = StrUtil.toUnderlineCase(fieldWithGet.substring(3));

		TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
		ObjectUtil.checkConditionThrowError(ObjectUtil.isNotNullEmpty(tableInfo),"领域模型映射关系不存在");

		// 命中主键属性：返回主键列名
		if (tableInfo.getKeyProperty().equals(propertyName)) {
			return tableInfo.getKeyColumn();
		}

		// 遍历普通字段映射：属性名匹配时返回映射列名
		for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
			if (fieldInfo.getProperty().equals(propertyName)) {
				return fieldInfo.getColumn();
			}
		}

		return propertyName;

	}


    /**
     * 批量获取数据库列名
     *
     * @param <O> 实体类型
     * @param functions Lambda 函数引用数组
     * @return 数据库列名数组
     */
    @SafeVarargs
    protected final <O> String[] getColumns(SFunction<O, ?>... functions) {

    	String [] columns = new String[functions.length];

		int i = 0;
    	for(SFunction<?, ?> sfunction : functions) {

			columns[i] = getColumn(sfunction);

	        ++i;

		}

    	return columns;

	}

    /**
     * 根据 Class 对象获取其对应的所有数据库列名（默认为关联查询模式）
     *
     * @param clazz Class 对象
     * @return 数据库列名数组
     */
	protected String[] getColumns(Class<?> clazz){
		return getColumns(clazz,true);
	}

    /**
     * 根据 Class 对象获取其对应的所有数据库列名
     * <p>
     * 遍历类及其父类的所有字段，根据 QueryMapping 注解决定列名的映射规则：
     * <ul>
     *   <li>无注解：直接使用字段名的下划线形式</li>
     *   <li>注解标记 ignore 且指定了 field 或 tableName：使用注解配置的列名</li>
     *   <li>其余带注解字段（未标记 ignore，或标记 ignore 但未指定 field 与 tableName）：跳过，不生成查询列</li>
     * </ul>
     * </p>
     *
     * @param clazz Class 对象
     * @param isJoin 是否为关联查询模式（关联查询时列名会添加表别名前缀）
     * @return 数据库列名数组
     */
	protected String[] getColumns(Class<?> clazz,Boolean isJoin) {

		List<String> columns = CollectionUtil.newArrayList();

		for( ; !Object.class.getName().equals(clazz.getName()); clazz = clazz.getSuperclass()){

			Field [] fields = clazz.getDeclaredFields();
			for(Field field : fields) {

				if(SerializeUtil.SERIAL_VERSION_UID_FIELD_NAME.equals(field.getName())){
					continue;
				}

				QueryMapping annotation = field.getAnnotation(QueryMapping.class);
				if(ObjectUtil.isNullEmpty(annotation)) {

					// 无注解：字段名转下划线，关联模式下追加默认表别名前缀
					String fieldName = StrUtil.toUnderlineCase(field.getName());
					columns.add(isJoin ? QueryEnvironmentContext.DEFAULT_ORDER_KEY + "." + fieldName : fieldName);

				}else if(annotation.ignore() && (StringUtils.isNotBlank(annotation.field()) || StringUtils.isNotBlank(annotation.tableName()))){

				// ignore 且显式指定来源字段或表名：按注解配置生成列名，未指定的部分回退字段名/默认别名
				String tableName = StringUtils.isNotBlank(annotation.tableName()) ?
							annotation.tableName()
							:
							QueryEnvironmentContext.DEFAULT_ORDER_KEY;

					String fieldName = StringUtils.isNotBlank(annotation.field()) ? annotation.field() : StrUtil.toUnderlineCase(field.getName());

					fieldName = isJoin ? tableName + "." + fieldName : fieldName;

					columns.add(fieldName);

				}

			}

		}

		return columns.toArray(new String[0]);

	}

    /**
     * 根据 ID 查询实体（指定字符串列名）
     *
     * @param id 实体 ID
     * @param columns 查询字段列表
     * @return 实体对象，不存在返回 null
     */
    protected T getById(Long id,String ... columns){

		ObjectUtil.isNullEmptyThrowError(id, getResourceName());

		QueryWrapper<T> wrapper = Wrappers.query();
		if(columns.length > 0) {
    		wrapper.select(columns);
    	}

    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);

    	return getOne(wrapper);

	}

    /**
     * 根据 ID 查询实体（指定 Lambda 列名）
     *
     * @param id 实体 ID
     * @param columns Lambda 函数引用指定的查询字段
     * @return 实体对象，不存在返回 null
     */
    @SafeVarargs
    protected final T getById(Long id, SFunction<T, ?>... columns){

		ObjectUtil.isNullEmptyThrowError(id, getResourceName());

		LambdaQueryWrapper<T> wrapper = Wrappers.lambdaQuery();
		if(columns.length > 0) {
    		wrapper.select(columns);
    	}

    	wrapper.eq(AbstractBaseDomain::getId,id);

    	return getOne(wrapper);

	}

    /**
     * 检查单个实体是否可用（非禁用状态）
     *
     * @param id 实体 ID
     * @throws BusinessResultException 当实体不存在或状态为禁用时抛出异常
     */
    protected void checkAvailable(Long id){

		ObjectUtil.isNullEmptyThrowError(id, getResourceName());

		QueryWrapper<T> wrapper = Wrappers.query();

		wrapper.select(getColumn(AbstractBaseDomain::getStatus));

    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);

    	T domain = getOne(wrapper);

		ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

		if(domain.getStatus() == Status.DISABLE) {
			throw new BusinessResultException(SystemErrorCode.FORBIDDEN_ERROR)
					.setResource(getResourceName());
		}

	}

    /**
     * 批量检查实体是否可用（非禁用状态）
     * <p>
     * 仅校验数据库中实际存在的实体状态，不校验 ID 是否存在
     * （对照：单个 ID 重载 {@link #checkAvailable(Long)} 会校验存在性）；
     * ID 集合为空时直接抛出数据不存在异常。
     * </p>
     *
     * @param ids 实体 ID 列表
     * @throws BusinessResultException 当任一已存在实体状态为禁用时抛出异常
     */
    protected void checkAvailable(List<Long> ids){

    	CollectionUtil.collectionIsEmptyThrowError(ids,getResourceName() + "列表");

		QueryWrapper<T> wrapper = Wrappers.query();

		wrapper.select(getColumn(AbstractBaseDomain::getStatus));

    	wrapper.in(getColumn(AbstractBaseDomain::getId),ids);

    	List<T> domains = list(wrapper);
    	domains.forEach((domain)->{

    		if(domain.getStatus() == Status.DISABLE) {
				throw new BusinessResultException(SystemErrorCode.FORBIDDEN_ERROR)
						.setResource(getResourceName());
        	}

    	});

	}

    /**
     * 根据 ID 查询可用实体（Lambda 列名方式），不可用则抛出异常
     *
     * @param id 实体 ID
     * @param columns Lambda 函数引用指定的查询字段
     * @return 可用的实体对象
     * @throws BusinessResultException 当实体不存在或状态为禁用时抛出异常
     */
	@SafeVarargs
    protected final T getAvailableById(Long id, SFunction<T, ?>... columns){
		return getAvailableById(id,getColumns(columns));
	}

    /**
     * 根据 ID 查询可用实体（字符串列名方式）
     * <p>
     * 自动追加 status 字段到查询列中，验证实体存在且状态为启用。
     * </p>
     *
     * @param id 实体 ID
     * @param columns 查询字段列表
     * @return 可用的实体对象
     * @throws BusinessResultException 当实体不存在或状态为禁用时抛出异常
     */
    protected T getAvailableById(Long id,String ... columns){

		ObjectUtil.isNullEmptyThrowError(id, getResourceName());

		QueryWrapper<T> wrapper = Wrappers.query();

		String[] newColumns = new String[columns.length + 1];
		System.arraycopy(columns, 0, newColumns, 0, columns.length);
		newColumns[columns.length] = getColumn(AbstractBaseDomain::getStatus);
    	wrapper.select(newColumns);

    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);

    	T domain = getOne(wrapper);

		ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

    	if(domain.getStatus() == Status.DISABLE) {
			throw new BusinessResultException(SystemErrorCode.FORBIDDEN_ERROR)
						.setResource(getResourceName());
    	}

    	return domain;

	}

    /**
     * 根据查询条件查询可用实体
     * <p>
     * 使用 MPJLambdaWrapper 构建查询，验证实体存在且状态为启用。
     * </p>
     *
     * @param wrapper MPJ Lambda 查询条件
     * @return 可用的实体对象
     * @throws BusinessResultException 当实体不存在或状态为禁用时抛出异常
     */
    protected T getAvailableByParam(MPJLambdaWrapper<T> wrapper){

		wrapper.selectAsClass(domainClass,domainClass);
    	wrapper.select(getColumn(AbstractBaseDomain::getStatus));

    	T domain = getOne(wrapper);

		ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

    	if(domain.getStatus() == Status.DISABLE) {
			throw new BusinessResultException(SystemErrorCode.FORBIDDEN_ERROR)
					.setResource(getResourceName());
    	}

    	return domain;

	}

    /**
     * 检查数据是否存在
     *
     * @param wrapper 查询条件
     * @throws BusinessResultException 当数据不存在时抛出异常
     */
    protected void checkHas(Wrapper<T> wrapper) {
		ErrorCode errorCode = SystemErrorCode.DATA_NOT_EXIST_ERROR;
		ObjectUtil.checkConditionThrowError(count(wrapper) > 0, errorCode, getResourceName() + errorCode.getMessage());
	}

    /**
     * 检查数据是否不存在
     *
     * @param wrapper 查询条件
     * @throws BusinessResultException 当数据已存在时抛出异常
     */
    protected void checkNotHas(Wrapper<T> wrapper) {
		ErrorCode errorCode = SystemErrorCode.DATA_EXIST_ERROR;
		ObjectUtil.checkConditionThrowError(count(wrapper) < 1, errorCode, getResourceName() + errorCode.getMessage());
	}

    /**
     * 根据 ID 获取下拉选项
     *
     * @param id 实体 ID
     * @return 下拉选项结果
     * @throws BusinessResultException 当实体不存在时抛出异常
     */
    protected DropDownResult dropDown(Long id){

		ObjectUtil.isNullEmptyThrowError(id, getResourceName());

		QueryWrapper<T> wrapper = Wrappers.query();

		wrapper.select(getColumns(AbstractBaseNameDomain::getId,AbstractBaseNameDomain::getName));

    	wrapper.eq(getColumn(AbstractBaseDomain::getId), id);

    	T domain = getOne(wrapper);

		ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

    	return CglibUtil.copy(domain, DropDownResult.class);

	}

    /**
     * 获取所有启用状态的下拉选项列表
     *
     * @return 下拉选项列表
     */
    protected List<DropDownResult> dropDownList(){

		QueryWrapper<T> wrapper = Wrappers.query();

		wrapper.select(getColumns(AbstractBaseNameDomain::getId,AbstractBaseNameDomain::getName));

    	wrapper.eq(getColumn(AbstractBaseDomain::getStatus), Status.ENABLE);

    	return CglibUtil.copyList(list(wrapper), DropDownResult::new);

	}

    /**
     * 根据 ID 查询实体，不存在则抛出异常（字符串列名方式）
     *
     * @param id 实体 ID
     * @param columns 查询字段列表
     * @return 实体对象
     * @throws BusinessResultException 当实体不存在时抛出异常
     */
    protected T getByIdThrowError(Long id,String ... columns){

		ObjectUtil.isNullEmptyThrowError(id, getResourceName());

		QueryWrapper<T> wrapper = Wrappers.query();

		if(columns.length > 0) {
    		wrapper.select(columns);
    	}

    	wrapper.eq(getColumn(AbstractBaseDomain::getId),id);

    	T domain = getOne(wrapper);

		ObjectUtil.isNullEmptyThrowError(domain,getResourceName());

    	return domain;

	}

    /**
     * 根据 ID 查询实体（使用 MPJLambdaWrapper），不存在则抛出异常
     *
     * @param id 实体 ID
     * @param wrapper MPJ Lambda 查询条件
     * @return 实体对象
     * @throws BusinessResultException 当实体不存在时抛出异常
     */
	protected T getByIdThrowError(Long id,MPJLambdaWrapper<T> wrapper){

		ObjectUtil.isNullEmptyThrowError(id, getResourceName());

		wrapper.eq(getColumn(AbstractBaseDomain::getId),id);

		T domain = getOne(wrapper);

		ObjectUtil.isNullEmptyThrowError(domain,getResourceName());

		return domain;

	}

    /**
     * 根据查询条件查询单个实体并转换为指定类型，不存在则抛出异常
     *
     * @param <R> 目标类型
     * @param wrapper 查询条件
     * @param clazz 目标类型 Class 对象
     * @return 转换后的对象
     * @throws BusinessResultException 当实体不存在时抛出异常
     */
    protected <R> R getByParamThrowError(Wrapper<T> wrapper,Class<R> clazz) {

    	T domain = getOne(wrapper);

		ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

		return CglibUtil.copy(domain, clazz);

	}

    /**
     * 根据查询条件查询单个实体并转换为指定类型，同时填充关联数据，不存在则抛出异常
     *
     * @param <R> 目标类型
     * @param wrapper 查询条件
     * @param clazz 目标类型 Class 对象
     * @return 转换后且填充了关联数据的对象
     * @throws BusinessResultException 当实体不存在时抛出异常
     */
    protected <R> R getByParamThrowErrorRelation(Wrapper<T> wrapper,Class<R> clazz) {

    	T domain = getOne(wrapper);

		ObjectUtil.isNullEmptyThrowError(domain, getResourceName());

		R response = CglibUtil.copy(domain, clazz);

		setRelationData(response, clazz);

		return response;

	}

    /**
     * 根据 ID 查询实体（Lambda 列名方式），不存在则抛出异常
     *
     * @param <R> 实体类型
     * @param id 实体 ID
     * @param columns Lambda 函数引用指定的查询字段
     * @return 实体对象
     * @throws BusinessResultException 当实体不存在时抛出异常
     */
    @SafeVarargs
    protected final <R> T getByIdThrowError(Long id, SFunction<R, ?> ... columns) {
    	return getByIdThrowError(id,getColumns(columns));
	}

    /**
     * 根据 ID 查询实体并转换为指定类型，不存在则抛出异常
     *
     * @param <V> 目标类型
     * @param <R> 实体类型
     * @param id 实体 ID
     * @param clazz 目标类型 Class 对象
     * @param columns Lambda 函数引用指定的查询字段
     * @return 转换后的对象
     * @throws BusinessResultException 当实体不存在时抛出异常
     */
    @SafeVarargs
    protected final <V,R> V getByIdThrowError(Long id, Class<V> clazz, SFunction<R, ?> ... columns){
    	return CglibUtil.copy(getByIdThrowError(id,columns), clazz);
	}

    /**
     * 根据 ID 查询实体并转换为指定类型，同时填充关联数据，不存在则抛出异常
     *
     * @param <V> 目标类型
     * @param id 实体 ID
     * @param clazz 目标类型 Class 对象
     * @return 转换后且填充了关联数据的对象
     * @throws BusinessResultException 当实体不存在时抛出异常
     */
    protected <V> V getByIdThrowErrorRelation(Long id,Class<V> clazz) {

		V response = CglibUtil.copy(getByIdThrowError(id,getColumns(clazz,false)), clazz);

    	setRelationData(response, clazz);

    	return response;

	}

    /**
     * 根据 DTO 参数查询实体并转换为指定类型，同时填充关联数据，不存在则抛出异常
     *
     * @param <V> 目标类型
     * @param <R> DTO 参数类型
     * @param dto 查询参数
     * @param clazz 目标类型 Class 对象
     * @return 转换后且填充了关联数据的对象
     * @throws BusinessResultException 当查询参数为空或实体不存在时抛出异常
     */
	protected <V,R> V getByParamThrowErrorRelation(R dto,Class<V> clazz) {

		ObjectUtil.isNullEmptyThrowError(dto, getResourceName());

		MPJLambdaWrapper<T> wrapper = buildQueryWrapper(dto,clazz);

		wrapper.select(getColumns(clazz));

		V response = CglibUtil.copy(getOne(wrapper), clazz);

		ObjectUtil.isNullEmptyThrowError(response, getResourceName());

		setRelationData(response, clazz);

		return response;

	}

    /**
     * 根据 ID 集合批量查询实体列表
     *
     * @param ids 实体 ID 集合
     * @param columns 查询字段列表
     * @return 实体列表
     * @throws BusinessResultException 当 ID 集合为空时抛出异常
     */
    protected List<T> listByIds(Collection<?> ids,String ... columns) {

		CollectionUtil.collectionIsEmptyThrowError(ids,getResourceName() + "列表");

		QueryWrapper<T> wrapper = Wrappers.query();
		if(columns.length > 0) {
    		wrapper.select(columns);
    	}

    	wrapper.in(getColumn(AbstractBaseDomain::getId),ids);

    	return list(wrapper);

	}

    /**
     * 查询指定类型的列表数据（使用 MPJ 关联查询）
     *
     * @param <R> Lambda 函数引用的实体类型
     * @param <V> 返回的 VO 类型
     * @param voClass VO 类型 Class 对象
     * @param columns 额外查询的 Lambda 函数引用字段
     * @return VO 列表
     */
    @SafeVarargs
    protected final <R,V> List<V> list(Class<V> voClass, SFunction<R, ?>... columns) {

    	MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
		wrapper.selectAsClass(domainClass,voClass);

		if(columns.length > 0) {
    		wrapper.select(columns);
    	}

		return selectJoinList(voClass, wrapper);

	}

    /**
     * 根据查询条件查询列表并转换为指定类型
     *
     * @param <R> 目标类型
     * @param wrapper 查询条件
     * @param target 目标类型的 Supplier
     * @return 转换后的列表
     */
    protected <R> List<R> list(Wrapper<T> wrapper,Supplier<R> target) {

    	List<T> list = list(wrapper);

    	return CglibUtil.copyList(list, target);

	}

    /**
     * 根据 DTO 参数查询列表并转换为指定类型
     *
     * @param <V> 目标类型
     * @param dto 查询参数
     * @param target 目标类型的 Supplier
     * @return 转换后的列表
     */
	protected <V> List<V> list(D dto,Supplier<V> target) {

		ObjectUtil.isNullEmptyThrowError(dto, getResourceName());

		V vo = target.get();

		List<T> list = list(buildQueryWrapper(dto, vo.getClass()));

		return CglibUtil.copyList(list, () -> vo);

	}

    /**
     * 根据 DTO 参数查询列表、填充关联数据并转换为指定类型
     * <p>
     * 注意：关联数据当前填充到查询参数 {@code dto} 对象上（见 {@code setRelationData(dto, ...)}），
     * 返回的列表元素本身不会被填充关联数据。
     * </p>
     *
     * @param <V> 目标类型
     * @param dto 查询参数
     * @param target 目标类型的 Supplier
     * @return 转换后的列表（未填充关联数据）
     */
	protected <V> List<V> listRelation(D dto,Supplier<V> target) {

		ObjectUtil.isNullEmptyThrowError(dto, getResourceName());

		V vo = target.get();

		List<T> list = list(buildQueryWrapper(dto,vo.getClass()));

		setRelationData(dto,vo.getClass());

		return CglibUtil.copyList(list, () -> vo);

	}

    /**
     * 分页查询并转换为指定类型
     *
     * @param <V> 目标类型
     * @param pageParam 分页查询参数
     * @param wrapper 查询条件
     * @param target 目标类型的 Supplier
     * @return 分页结果
     */
    protected <V> PageResult<V> list(PageParam<?> pageParam,Wrapper<T> wrapper,Supplier<V> target) {

    	Page<T> page = page(convertPage(pageParam),wrapper);

    	return convertPageResult(page, target);

	}

    /**
     * 根据分页参数和 VO 类型进行关联分页查询
     *
     * @param <V> VO 类型
     * @param pageParam 分页查询参数
     * @param clazz VO 类型 Class 对象
     * @return 分页结果
     */
    protected <V> PageResult<V> list(PageParam<?> pageParam,Class<V> clazz) {

		Page<V> page = selectJoinListPage(convertPage(pageParam),clazz,buildQueryWrapper(pageParam.getData(),clazz));

    	return convertPageResult(page);

	}

    /**
     * 根据分页参数和查询条件进行分页查询（返回原始 MyBatis-Plus 分页对象）
     *
     * @param pageParam 分页查询参数
     * @param wrapper 查询条件
     * @return MyBatis-Plus 分页对象
     */
    protected Page<T> list(PageParam<?> pageParam,Wrapper<T> wrapper) {
		return page(convertPage(pageParam),wrapper);
	}

    /**
     * 根据分页参数进行关联分页查询并填充关联数据
     *
     * @param <V> VO 类型
     * @param pageParam 分页查询参数
     * @param clazz VO 类型 Class 对象
     * @return 填充了关联数据的分页结果
     */
    protected <V> PageResult<V> listRelation(PageParam<?> pageParam,Class<V> clazz) {

    	PageResult<V> page = list(pageParam,clazz);

		setRelationData(page, clazz);

		return page;

	}

    /**
     * 根据参数统计符合条件的记录数
     *
     * @param <P> 参数类型
     * @param param 查询参数
     * @return 符合条件的记录数
     */
	protected <P> Long getCountByParam(P param){

		ObjectUtil.isNullEmptyThrowError(param,getResourceName());

		return count(buildQueryWrapper(param));

	}

    /**
     * 新增或更新实体
     * <p>
     * 如果实体 ID 不为空，则先检查数据是否存在后再更新；
     * 如果实体 ID 为空，则执行新增操作。
     * </p>
     *
     * @param domain 实体对象
     * @return 操作是否成功
     */
    public boolean saveUpdate(T domain) {

		ObjectUtil.isNullEmptyThrowError(domain,getResourceName());

    	// 携带 ID 视为更新：先校验数据存在
    	if(ObjectUtil.isNotNullEmpty(domain.getId())) {

			QueryWrapper<T> wrapper = Wrappers.query();

			wrapper.eq(getColumn(AbstractBaseDomain::getId), domain.getId());

			ErrorCode errorCode = SystemErrorCode.DATA_NOT_EXIST_ERROR;
			ObjectUtil.checkConditionThrowError(count(wrapper) > 0, errorCode, getResourceName() + errorCode.getMessage());

    	}

		return super.saveOrUpdate(domain);

	}

    /**
     * 新增或更新实体（通过参数对象）
     * <p>
     * 将参数对象拷贝为领域模型后执行新增或更新操作。
     * 更新时校验数据存在性，新增时校验参数有效性。
     * </p>
     *
     * @param id 实体 ID（为空时执行新增）
     * @param param 参数对象
     * @return 新增或更新后的实体 ID
     */
	public Long saveUpdate(Long id,Object param) {

		ObjectUtil.isNullEmptyThrowError(param,getResourceName());

    	T domain = CglibUtil.copy(param, domainClass);

    	// 携带 ID 视为更新：回填 ID 并校验数据存在
    	if(ObjectUtil.isNotNullEmpty(id)) {

    		domain.setId(id);

			QueryWrapper<T> wrapper = Wrappers.query();
			wrapper.eq(getColumn(AbstractBaseDomain::getId), domain.getId());

			ErrorCode errorCode = SystemErrorCode.DATA_NOT_EXIST_ERROR;
			ObjectUtil.checkConditionThrowError(count(wrapper) > 0, errorCode, getResourceName() + errorCode.getMessage());

    	}else {
    		// 未携带 ID 视为新增：校验参数合法性
    		ValidatorUtil.validate(param);
    	}

    	super.saveOrUpdate(domain);

    	return domain.getId();

	}

    /**
     * 批量新增或更新实体
     * <p>
     * 串行遍历批量数据，每条数据独立校验存在性（携带 ID 时）后逐条执行新增或更新。
     * </p>
     *
     * @param domains 实体列表
     */
    public void saveUpdateBatch(List<T> domains) {

		CollectionUtil.collectionIsEmptyThrowError(domains,getResourceName() + "列表");

		Consumer<T> consumer = (domain) -> {

			// 携带 ID 视为更新：先校验数据存在
			if(ObjectUtil.isNotNullEmpty(domain.getId())) {

				QueryWrapper<T> wrapper = Wrappers.query();
				wrapper.eq(getColumn(AbstractBaseDomain::getId), domain.getId());

				ErrorCode errorCode = SystemErrorCode.DATA_NOT_EXIST_ERROR;
				ObjectUtil.checkConditionThrowError(count(wrapper) > 0, errorCode, getResourceName() + errorCode.getMessage());

			}

			super.saveOrUpdate(domain);

		};

		domains.forEach(Objects.requireNonNull(consumer));

    }

    /**
     * 检查实体是否存在绑定关系（供子类覆盖）
     *
     * @param id 实体 ID
     */
    protected void checkBind(Long id) {};

    /**
     * 解除实体的绑定关系（供子类覆盖）
     *
     * @param id 实体 ID
     */
    protected void unbind(Long id) {};

    /**
     * 根据 ID 删除实体
     * <p>
     * 删除前先检查绑定关系并解除绑定。
     * </p>
     *
     * @param id 实体 ID
     */
    public void removeById(Long id) {

		ObjectUtil.isNullEmptyThrowError(id,getResourceName());

		checkBind(id);

		unbind(id);

		super.removeById(id);

    }





//	========================== 结果集封装 ==========================

    /**
     * 根据 ID 查询实体并封装为结果（字符串列名方式）
     *
     * @param id 实体 ID
     * @param columns 查询字段列表
     * @return 包含 DTO 的统一响应结果
     */
    @Override
    public AbstractBaseResult<D> getByIdResult(Long id,String ... columns) {
		T domain = getById(id, columns);
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
	public AbstractBaseResult<Map<Long,D>> listByIdsResult(Collection<Long> ids,String ... columns) {

		List<T> domains = listByIds(ids, columns);

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
     * 根据关联字段分组查询并封装为 Map 结果
     *
     * @param ids 关联 ID 集合
     * @param relationField 关联字段名
     * @param columns 查询字段列表
     * @return 包含 Map&lt;Long, List&lt;D&gt;&gt; 的统一响应结果
     */
    @Override
	public AbstractBaseResult<Map<Long,List<D>>> listByRelationIdsResult(Collection<Long> ids,String relationField,String ... columns) {

		CollectionUtil.collectionIsEmptyThrowError(ids,getResourceName() + "列表");

		ObjectUtil.isNullEmptyThrowError(relationField, getResourceName() + "分组");

		QueryWrapper<T> wrapper = Wrappers.query();
		if(columns.length > 0) {
    		wrapper.select(columns);
    	}
    	wrapper.in(relationField,ids);

		List<T> domains = list(wrapper);
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
     * 根据单个关联 ID 查询关联列表并封装为结果
     *
     * @param id 关联 ID
     * @param relationField 关联字段名
     * @param columns 查询字段列表
     * @return 包含 List&lt;D&gt; 的统一响应结果
     */
    @Override
	public AbstractBaseResult<List<D>> listByRelationIdResult(Long id,String relationField,String ... columns) {

		ObjectUtil.isNullEmptyThrowError(id,getResourceName());

		ObjectUtil.isNullEmptyThrowError(relationField, getResourceName() + "分组");

		QueryWrapper<T> wrapper = Wrappers.query();
		if(columns.length > 0) {
    		wrapper.select(columns);
    	}
    	wrapper.eq(relationField,id);

		return ResultUtil.success(CglibUtil.copyList(list(wrapper), () -> ReflectUtil.newInstance(getResultClass())));

	}

    /**
     * 根据参数统计记录数并封装为结果
     *
     * @param dto 查询参数
     * @return 包含记录数的统一响应结果
     */
    @Override
	public AbstractBaseResult<Long> getCountByParamResult(D dto){
		return ResultUtil.success(getCountByParam(dto));
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
	@Transactional
	public <P> AbstractBaseResult<Long> saveUpdateResult(Long id, P param){
		return ResultUtil.success(saveUpdate(id,param));
	}

    /**
     * 批量新增或更新并封装为结果
     *
     * @param collection 新增/更新参数列表
     * @return 统一响应结果
     */
	@Override
	@Transactional
	public AbstractBaseResult<Void> saveUpdateBatchResult(List<SaveUpdateBatch> collection){

		CollectionUtil.collectionIsEmptyThrowError(collection, getResourceName() + "列表");

		List<T> domains = CglibUtil.copyList(collection, ()-> ReflectUtil.newInstance(domainClass));
		saveUpdateBatch(domains);

		return ResultUtil.success();

	}

    /**
     * 根据 ID 删除实体并封装为结果
     *
     * @param id 实体 ID
     * @return 统一响应结果
     */
	@Override
	@Transactional
	public AbstractBaseResult<Void> removeByIdResult(Long id) {
		removeById(id);
		return ResultUtil.success();
	}

    /**
     * 批量删除实体并封装为结果
     *
     * @param ids 实体 ID 列表
     * @return 统一响应结果
     */
	@Override
	@Transactional
	public AbstractBaseResult<Void> removeByIdBatchResult(List<Long> ids) {

		CollectionUtil.collectionIsEmptyThrowError(ids, getResourceName() + "列表");

		ids.forEach(this::removeById);

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
		checkAvailable(id);
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
		checkAvailable(ids);
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

    	T domain = getAvailableById(id, columns);

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
		return ResultUtil.success(dropDown(id));
	}

    /**
     * 获取所有启用状态的下拉选项列表并封装为结果
     *
     * @return 包含下拉选项列表的统一响应结果
     */
	public AbstractBaseResult<List<DropDownResult>> dropDownListResult(){
		return ResultUtil.success(dropDownList());
	}

    /**
     * 根据 ID 集合批量获取下拉选项并封装为结果
     *
     * @param ids 实体 ID 集合
     * @return 包含下拉选项列表的统一响应结果
     */
	@Override
	public AbstractBaseResult<List<DropDownResult>> dropDownListResult(Collection<Long> ids){

		QueryWrapper<T> wrapper = Wrappers.query();

		wrapper.select(getColumns(AbstractBaseNameDomain::getId,AbstractBaseNameDomain::getName));

    	wrapper.in(getColumn(AbstractBaseDomain::getId), ids);

    	return ResultUtil.success(CglibUtil.copyList(list(wrapper), DropDownResult::new));

	}

    /**
     * 更新实体状态
     * <p>
     * 目标状态为禁用（DISABLE）时先调用 {@link #checkBind(Long)} 检查绑定关系；
     * 随后仅设置 ID 与状态字段构造新实体并按 ID 更新
     * （MyBatis-Plus 默认只更新非空字段，即仅变更状态列）。
     * </p>
     *
     * @param id 实体 ID
     * @param param 携带目标状态的请求参数（data 为空时抛出参数错误异常）
     * @return 统一响应结果
     */
	@SneakyThrows
	public AbstractBaseResult<Void> updateStatusResult(Long id, RequestParam<Status> param){

		Status status = param.getData();
		ObjectUtil.isNullEmptyThrowError(status,SystemErrorCode.PARAM_ERROR,"状态");

		// 目标状态为禁用时，需先检查是否存在绑定关系
		if(param.getData() == Status.DISABLE){
			checkBind(id);
		}

		T domain = domainClass.getDeclaredConstructor().newInstance();

		domain.setId(id);
		domain.setStatus(status);

		updateById(domain);

		return ResultUtil.success();

	}




//	====================== 工具方法 ======================

    /**
     * 根据参数对象的注解信息构建查询条件
     * <p>
     * 处理流程：
     * <ol>
     *   <li>解析类级别的 JoinPlugins 注解，构建关联查询条件</li>
     *   <li>遍历参数对象的所有字段（值为空的字段跳过），根据 QueryPlugin 注解构建查询条件</li>
     *   <li>处理注解标记 nest=true 的嵌套参数对象，以注解指定的表名递归解析其内部条件</li>
     *   <li>处理 TimeParam 类型的字段，自动生成范围查询条件</li>
     * </ol>
     * </p>
     *
     * @param tableName 表别名
     * @param param 查询参数对象
     * @param wrapper MPJ Lambda 查询条件构造器
     */
	private void paramFieldSetWrapper(String tableName, Object param, MPJLambdaWrapper<T> wrapper) {

		Class<?> paramClass = param.getClass();

		// 解析类级 JoinPlugins 注解：逐个校验连接类型并应用关联插件拼接 JOIN
		if (paramClass.isAnnotationPresent(JoinPlugins.class)) {

			JoinPlugins annotations = paramClass.getAnnotation(JoinPlugins.class);

			if(ObjectUtil.isNotNullEmpty(annotations.value())){

				Arrays.stream(annotations.value()).forEach(annotation -> {

					JoinBaseType value = annotation.value();

					ObjectUtil.isNullEmptyThrowError(value, getResourceName() + "连接类型");

					ObjectUtil.isNullEmptyThrowError(value.getType(), getResourceName() + "连接类型");

					JoinType joinType = JoinType.get(value.getType());

					ObjectUtil.isNullEmptyThrowError(joinType, getResourceName() + "连接类型");

					Objects.requireNonNull(joinType).getJoinPlugin().apply(getResourceName(), wrapper, annotation);

				});

			}

		}

		Field [] fields = ReflectUtil.getFields(param.getClass());

		for(Field field : fields) {

			if(SerializeUtil.SERIAL_VERSION_UID_FIELD_NAME.equals(field.getName())){
				continue;
			}

			Object data = ReflectUtil.invokeGet(param, field.getName());
			// 字段值为空时不参与查询条件构建，直接跳过
			if(ObjectUtil.isNullEmpty(data)) {continue;}

			QueryPlugin annotation = field.getAnnotation(QueryPlugin.class);

			String fieldName = ObjectUtil.isNotNullEmpty(annotation) && StringUtils.isNotBlank(annotation.field()) ?
					annotation.field()
					:
					field.getName();

			if(ObjectUtil.isNotNullEmpty(annotation)) {

				// 嵌套参数：以注解指定的表名递归解析其内部字段的查询条件
				if(annotation.nest()) {

					paramFieldSetWrapper(annotation.tableName(),field,wrapper);

					continue;

				}

				QueryBaseType value = annotation.value();

				ObjectUtil.isNullEmptyThrowError(value, getResourceName() + "查询类型");

				ObjectUtil.isNullEmptyThrowError(value.getType(), getResourceName() + "查询类型");

				QueryType queryType = QueryType.get(value.getType());

				ObjectUtil.isNullEmptyThrowError(queryType, getResourceName() + "查询类型");

                // 字段注解显式指定表名时，覆盖外层传入的表名
                if(StringUtils.isNotBlank(annotation.tableName())){
                    tableName = annotation.tableName();
                }

				if(StringUtils.isNotBlank(tableName)){
					fieldName = tableName + "." + fieldName;
				}

				Objects.requireNonNull(queryType).getQueryPlugin().apply(wrapper, fieldName, data);

			}

			// 时间范围参数：起始时间拼 >= 条件、结束时间拼 <= 条件（不依赖 QueryPlugin 注解）
			if(data instanceof TimeParam timeParam) {

				if(ObjectUtil.isNotNullEmpty(timeParam.getStartTime())) {
					QueryType.GE.getQueryPlugin().apply(wrapper, fieldName, timeParam.getStartTime());
				}

				if(ObjectUtil.isNotNullEmpty(timeParam.getEndTime())) {
					QueryType.LE.getQueryPlugin().apply(wrapper, fieldName, timeParam.getEndTime());
				}

			}


		}

	}

    /**
     * 设置查询映射关联（MPJ 关联查询的 selectCollection/selectAssociation）
     * <p>
     * 遍历 VO 类中带有 QueryMapping 注解且未标记 ignore、isService 的字段，
     * 根据注解配置自动构建 MyBatis-Plus-Join 的 selectCollection 或 selectAssociation 映射。
     * </p>
     *
     * @param clazz VO 类型 Class 对象
     * @param wrapper MPJ Lambda 查询条件构造器
     */
	protected void setQueryMappingWrapper(Class<?> clazz, MPJLambdaWrapper<T> wrapper){

		for(;!Object.class.getName().equals(clazz.getName());clazz = clazz.getSuperclass()){

			Field [] fields = clazz.getDeclaredFields();

			for(Field field : fields) {

				QueryMapping annotation = field.getAnnotation(QueryMapping.class);
				if(ObjectUtil.isNotNullEmpty(annotation) && !annotation.ignore() && !annotation.isService()) {

					Class<?> type = field.getType();

					String tableName = annotation.tableName();
					ObjectUtil.checkConditionThrowError(ObjectUtil.isNotNullEmpty(tableName),getResourceName() + "映射关系名称");

					Class<?> relationClazz = StoreClass.storeClassMap.get(StoreClassType.DB).get(tableName);
					ObjectUtil.checkConditionThrowError(ObjectUtil.isNotNullEmpty(relationClazz),getResourceName() + "映射关系不存在");

					ObjectUtil.checkConditionThrowError(!type.isPrimitive(),getResourceName() + "映射关系类型错误");

					if(type.equals(List.class)){
						wrapper.selectCollection(relationClazz,(response) -> ReflectUtil.invokeGet(response, field.getName()));
					}else{
						wrapper.selectAssociation(relationClazz,(response) -> ReflectUtil.invokeGet(response, field.getName()));
					}

				}

			}

		}

	}

    /**
     * 构建关联查询条件（指定 VO 类型）
     * <p>
     * 根据参数对象和 VO 类型构建 MPJLambdaWrapper，自动处理：
     * <ul>
     *   <li>select 字段映射</li>
     *   <li>基于 QueryPlugin 注解的动态查询条件</li>
     *   <li>基于 OrderParam 的排序条件</li>
     * </ul>
     * </p>
     *
     * @param param 查询参数对象（支持 PageParam 自动解包）
     * @param fieldsClass VO 类型 Class 对象
     * @return MPJ Lambda 查询条件构造器
     */
	public MPJLambdaWrapper<T> buildQueryWrapper(Object param,Class<?> fieldsClass) {

		if(param instanceof PageParam<?> pageParam) {
			param = pageParam.getData();
		}

		ObjectUtil.isNullEmptyThrowError(param, getResourceName() + "参数");

		MPJLambdaWrapper<T> wrapper = MPJWrappers.lambdaJoin();
		wrapper.selectAsClass(domainClass, fieldsClass);

		if(ObjectUtil.isNotNullEmpty(fieldsClass)) {

			wrapper.select(getColumns(fieldsClass));

//			setQueryMappingWrapper(fieldsClass,wrapper);

		}

		paramFieldSetWrapper(null,param,wrapper);

		// 排序参数：初始化默认排序后，按默认表别名取升序/降序字段拼接 ORDER BY
		if(param instanceof OrderParam orderParam) {

			orderParam.initOrderParam();

			List<String> asc = orderParam.getOrderAscFieldsMap().get(QueryEnvironmentContext.DEFAULT_ORDER_KEY);

			List<String> desc = orderParam.getOrderDescFieldsMap().get(QueryEnvironmentContext.DEFAULT_ORDER_KEY);

			wrapper.orderByAscStr(ObjectUtil.isNotNullEmpty(asc),asc);

			wrapper.orderByDescStr(ObjectUtil.isNotNullEmpty(desc),desc);

		}

		return wrapper;

	}

    /**
     * 构建关联查询条件（不指定 VO 类型）
     *
     * @param param 查询参数对象
     * @return MPJ Lambda 查询条件构造器
     */
	public MPJLambdaWrapper<T> buildQueryWrapper(Object param) {
		return buildQueryWrapper(param,null);
	}

	/**
	 * 设置关联数据
	 * <p>
	 * 根据 VO 类中 QueryMapping 注解的配置，通过 RPC 或本地服务调用
	 * 自动填充关联数据。支持以下关联类型：
	 * <ul>
	 *   <li>通过 functionName 自定义方法调用</li>
	 *   <li>List 类型：一对多关联</li>
	 *   <li>DropDownResult 类型：下拉选项关联</li>
	 *   <li>其他类型：一对一关联</li>
	 * </ul>
	 * </p>
	 *
	 * @param data 响应数据对象（支持 PageResult、List 和单个对象）
	 * @param clazz VO 类型 Class 对象
	 */
	public void setRelationData(Object data, Class<?> clazz) {

		if (data instanceof PageResult<?> pageResult) {
			data = pageResult.getData();
		}

		if (data instanceof List<?> responses) {
			setBatchRelationData(responses, clazz);
		} else {
			setSingleRelationData(data, clazz);
		}

	}

	/**
	 * 解析并校验服务 Bean
	 *
	 * @param annotation QueryMapping 注解
	 * @return 服务 Bean 对象
	 */
	private Object resolveServiceBean(QueryMapping annotation) {
		Object service = SpringUtil.getBean(annotation.serviceName());
		ObjectUtil.checkConditionThrowError(ObjectUtil.isNotNullEmpty(service), getResourceName() + "服务不存在");
		return service;
	}

	/**
	 * 批量设置关联数据
	 * <p>
	 * 遍历 VO 类中所有带 QueryMapping 注解的字段，批量收集 ID 后统一调用服务获取关联数据。
	 * </p>
	 *
	 * @param responses 响应数据列表
	 * @param clazz VO 类型 Class 对象
	 */
	private void setBatchRelationData(List<?> responses, Class<?> clazz) {

		if (ObjectUtil.isNullEmpty(responses)) {
			return;
		}

		Field[] fields = ReflectUtil.getFields(clazz);
		for (Field field : fields) {

			QueryMapping annotation = field.getAnnotation(QueryMapping.class);
			if (ObjectUtil.isNullEmpty(annotation) || annotation.ignore()) {
				continue;
			}

			String functionName = annotation.functionName();
			String relationFieldName = annotation.relationField();
			ObjectUtil.checkConditionThrowError(
				ObjectUtil.isNotNullEmpty(relationFieldName) || ObjectUtil.isNotNullEmpty(functionName),
				getResourceName() + "关系字段未设置"
			);

			if (annotation.isService()) {
				Object service = resolveServiceBean(annotation);
				setBatchRelationField(responses, field, service, functionName, relationFieldName);
			}

		}

	}

	/**
	 * 单个对象设置关联数据
	 * <p>
	 * 遍历 VO 类中所有带 QueryMapping 注解的字段，逐个调用服务获取关联数据。
	 * </p>
	 *
	 * @param data 响应数据对象
	 * @param clazz VO 类型 Class 对象
	 */
	private void setSingleRelationData(Object data, Class<?> clazz) {

		Field[] fields = ReflectUtil.getFields(clazz);
		for (Field field : fields) {

			QueryMapping annotation = field.getAnnotation(QueryMapping.class);
			if (ObjectUtil.isNullEmpty(annotation) || annotation.ignore()) {
				continue;
			}

			String functionName = annotation.functionName();
			String relationFieldName = annotation.relationField();
			ObjectUtil.checkConditionThrowError(
				ObjectUtil.isNotNullEmpty(relationFieldName) || ObjectUtil.isNotNullEmpty(functionName),
				getResourceName() + "关系字段未设置"
			);

			if (annotation.isService()) {
				Object service = resolveServiceBean(annotation);
				setSingleRelationField(data, field, service, functionName, relationFieldName);
			}

		}

	}

	/**
	 * 批量模式：设置单个字段的关联数据
	 * <p>
	 * 根据字段类型进行 4 路分发：functionName 自定义方法、List 一对多、DropDownResult 下拉、其他一对一。
	 * 批量收集 ID 后统一调用服务获取关联数据，再分发回各响应对象
	 * （一对多关联收集对象自身主键 ID 作为关联值，其余分支收集关联字段的值）。
	 * </p>
	 *
	 * @param responses 响应数据列表
	 * @param field 当前字段
	 * @param service 服务 Bean
	 * @param functionName 自定义方法名
	 * @param relationFieldName 关联字段名
	 */
	private void setBatchRelationField(List<?> responses, Field field, Object service,
			String functionName, String relationFieldName) {

		Class<?> type = field.getType();

		// 自定义方法分支：按关联字段收集参数统一调用，再按关联字段取回并分发结果
		if (StringUtils.isNotBlank(functionName)) {

			Set<Object> param = CollectionUtil.newHashSet();
			for (Object response : responses) {
				param.add(ReflectUtil.invokeGet(response, relationFieldName));
			}

			if (ObjectUtil.isNotNullEmpty(param)) {
				Map<Long, Object> result = ReflectUtil.invoke(service, functionName, param, relationFieldName);
				for (Object response : responses) {
					Long id = ReflectUtil.invokeGet(response, relationFieldName);
					Object object = result.get(id);
					if (ObjectUtil.isNotNullEmpty(object)) {
						ReflectUtil.invokeSet(response, field.getName(), object);
					}
				}
			}

		// 一对多分支：以对象自身主键 ID 作为关联值，按关联字段分组取回列表
		} else if (type.equals(List.class)) {

			Set<Long> relationIds = CollectionUtil.newHashSet();
			for (Object response : responses) {
				relationIds.add(ReflectUtil.invokeFunGetId(response));
			}

			if (ObjectUtil.isNotNullEmpty(relationIds)) {
				AbstractBaseResult<Map<Long, List<?>>> rpcResult = ReflectUtil.invoke(
					service, ResultFunName.LIST_BY_RELATION_IDS_RESULT, relationIds, relationFieldName, getColumns(field.getClass()));
				Map<Long, List<?>> resultData = rpcResult.getResultDataThrowError();
				for (Object response : responses) {
					Long id = ReflectUtil.invokeFunGetId(response);
					List<?> list = resultData.get(id);
					if (ObjectUtil.isNotNullEmpty(list)) {
						ReflectUtil.invokeSet(response, field.getName(), list);
					}
				}
			}

		// 下拉关联分支：按关联字段批量取回下拉选项，转 Map 后按 ID 分发
		} else if (type.equals(DropDownResult.class)) {

			Set<Long> relationIds = CollectionUtil.newHashSet();
			for (Object response : responses) {
				relationIds.add(ReflectUtil.invokeGet(response, relationFieldName));
			}

			if (ObjectUtil.isNotNullEmpty(relationIds)) {
				AbstractBaseResult<List<DropDownResult>> rpcResult = ReflectUtil.invoke(
					service, ResultFunName.DROP_DOWN_LIST_RESULT, relationIds);
				List<DropDownResult> resultData = rpcResult.getResultDataThrowError();
				Map<Long, DropDownResult> relationsMap = toMapByIds(resultData, DropDownResult::getId);
				for (Object response : responses) {
					Long id = ReflectUtil.invokeGet(response, relationFieldName);
					DropDownResult relation = relationsMap.get(id);
					if (ObjectUtil.isNotNullEmpty(relation)) {
						ReflectUtil.invokeSet(response, field.getName(), relation);
					}
				}
			}

		// 一对一分支：按关联字段批量取回关联对象，转 Map 后按 ID 分发
		} else {

			Set<Long> relationIds = CollectionUtil.newHashSet();
			for (Object response : responses) {
				relationIds.add(ReflectUtil.invokeGet(response, relationFieldName));
			}

			if (ObjectUtil.isNotNullEmpty(relationIds)) {
				AbstractBaseResult<Map<Long, Object>> rpcResult = ReflectUtil.invoke(
					service, ResultFunName.LIST_BY_IDS_RESULT, relationIds, getColumns(field.getClass()));
				Map<Long, Object> resultData = rpcResult.getResultDataThrowError();
				for (Object response : responses) {
					Long id = ReflectUtil.invokeGet(response, relationFieldName);
					Object relation = resultData.get(id);
					if (ObjectUtil.isNotNullEmpty(relation)) {
						ReflectUtil.invokeSet(response, field.getName(), relation);
					}
				}
			}

		}

	}

	/**
	 * 单对象模式：设置单个字段的关联数据
	 * <p>
	 * 根据字段类型进行 4 路分发：functionName 自定义方法、List 一对多、DropDownResult 下拉、其他一对一。
	 * 针对单个对象调用服务获取关联数据。
	 * </p>
	 *
	 * @param data 响应数据对象
	 * @param field 当前字段
	 * @param service 服务 Bean
	 * @param functionName 自定义方法名
	 * @param relationFieldName 关联字段名
	 */
	private void setSingleRelationField(Object data, Field field, Object service,
			String functionName, String relationFieldName) {

		Class<?> type = field.getType();

		// 自定义方法分支：配置了关联字段则携带其值调用，否则无参调用
		if (StringUtils.isNotBlank(functionName)) {

			Object result = null;
			if (StringUtils.isNotBlank(relationFieldName)) {
				var param = ReflectUtil.invokeGet(data, relationFieldName);
				result = ReflectUtil.invoke(service, functionName, param);
			} else {
				result = ReflectUtil.invoke(service, functionName);
			}

			if (ObjectUtil.isNotNullEmpty(result)) {
				ReflectUtil.invokeSet(data, field.getName(), result);
			}

		// 一对多分支：以对象自身主键作为关联值查询关联列表
		} else if (type.equals(List.class)) {

			Long id = ReflectUtil.invokeFunGetId(data);
			AbstractBaseResult<List<?>> rpcResult = ReflectUtil.invoke(
				service, ResultFunName.LIST_BY_RELATION_ID_RESULT, id, relationFieldName, getColumns(field.getClass()));
			List<?> resultData = rpcResult.getResultDataThrowError();
			if (ObjectUtil.isNotNullEmpty(resultData)) {
				ReflectUtil.invokeSet(data, field.getName(), resultData);
			}

		// 下拉关联分支：按关联字段值查询单个下拉选项
		} else if (type.equals(DropDownResult.class)) {

			Long relationId = ReflectUtil.invokeGet(data, relationFieldName);
			if (ObjectUtil.isNotNullEmpty(relationId)) {
				AbstractBaseResult<DropDownResult> rpcResult = ReflectUtil.invoke(
					service, ResultFunName.DROP_DOWN_RESULT, relationId);
				DropDownResult resultData = rpcResult.getResultDataThrowError();
				if (ObjectUtil.isNotNullEmpty(resultData)) {
					ReflectUtil.invokeSet(data, field.getName(), resultData);
				}
			}

		// 一对一分支：按关联字段值查询单个关联对象
		} else {

			Long relationId = ReflectUtil.invokeGet(data, relationFieldName);
			if (ObjectUtil.isNotNullEmpty(relationId)) {
				AbstractBaseResult<Object> rpcResult = ReflectUtil.invoke(
					service, ResultFunName.GET_BY_ID_RESULT, relationId, getColumns(field.getClass()));
				Object resultData = rpcResult.getResultDataThrowError();
				if (ObjectUtil.isNotNullEmpty(resultData)) {
					ReflectUtil.invokeSet(data, field.getName(), resultData);
				}
			}

		}

	}

}
