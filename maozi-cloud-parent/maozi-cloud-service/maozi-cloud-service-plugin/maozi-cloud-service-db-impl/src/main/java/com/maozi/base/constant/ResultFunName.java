package com.maozi.base.constant;

/**
 * 结果方法名常量
 * <p>
 * 定义 {@code BaseServiceResult} 中需反射调用的方法名字符串常量，
 * 用于填充关联数据时匹配目标方法。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/5/6
 */
public final class ResultFunName {

    /** 常量类私有构造，禁止实例化 */
    private ResultFunName() {}

    /** getByIdResult 方法名 */
    public static final String GET_BY_ID_RESULT = "getByIdResult";

    /** dropDownResult 方法名 */
    public static final String DROP_DOWN_RESULT = "dropDownResult";

    /** dropDownListResult 方法名 */
    public static final String DROP_DOWN_LIST_RESULT = "dropDownListResult";

    /** listByIdsResult 方法名 */
    public static final String LIST_BY_IDS_RESULT = "listByIdsResult";

    /** listByRelationIdResult 方法名 */
    public static final String LIST_BY_RELATION_ID_RESULT = "listByRelationIdResult";

    /** listByRelationIdsResult 方法名 */
    public static final String LIST_BY_RELATION_IDS_RESULT = "listByRelationIdsResult";

}
