package com.maozi.db.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 逗号分隔字符串与集合的双向转换 TypeHandler
 * <p>
 * 将数据库中以逗号分隔的字符串（如 "a,b,c"）与 {@link Set}{@code <String>} 或 {@link List}{@code <String>} 互转，
 * 按字段声明类型返回对应集合实现：声明为 Set 返回 {@link LinkedHashSet}，声明为 List 返回 {@link ArrayList}。
 * 读取时去除元素首尾空格并过滤空元素。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@MappedTypes({Collection.class, List.class, Set.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class CommaSeparatedTypeHandler extends BaseTypeHandler<Collection<String>> {

    /** 逗号分隔符常量 */
    private static final String DELIMITER = ",";

    /** 字段声明的集合类型，用于决定返回 Set 还是 List */
    private final Class<?> declaredType;

    /**
     * 构造方法，传入字段声明的集合类型
     *
     * @param type 字段声明的集合类型（Set 或 List）
     */
    public CommaSeparatedTypeHandler(Class<?> type) {
        this.declaredType = type;
    }

    /**
     * 设置非空参数，将 Java 集合转为逗号分隔的字符串存入数据库
     *
     * @param ps        预编译语句
     * @param i         参数索引位置
     * @param parameter 要设置的集合参数
     * @param jdbcType  JDBC 类型
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Collection<String> parameter, JdbcType jdbcType) throws SQLException {
        // 使用逗号将集合中的元素拼接成一个字符串
        ps.setString(i, String.join(DELIMITER, parameter));
    }

    /**
     * 根据列名从结果集中获取逗号分隔字符串并转为集合
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return 字符串集合
     * @throws SQLException SQL 异常
     */
    @Override
    public Collection<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toCollection(rs.getString(columnName));
    }

    /**
     * 根据列索引从结果集中获取逗号分隔字符串并转为集合
     *
     * @param rs          结果集
     * @param columnIndex 列索引
     * @return 字符串集合
     * @throws SQLException SQL 异常
     */
    @Override
    public Collection<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toCollection(rs.getString(columnIndex));
    }

    /**
     * 从存储过程结果中根据列索引获取逗号分隔字符串并转为集合
     *
     * @param cs          存储过程语句
     * @param columnIndex 列索引
     * @return 字符串集合
     * @throws SQLException SQL 异常
     */
    @Override
    public Collection<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toCollection(cs.getString(columnIndex));
    }

    /**
     * 将逗号分隔的字符串转换为集合
     * <p>
     * 根据字段声明的类型（Set 或 List）返回对应的集合实现。
     * 空值或空白字符串返回空集合。每个元素会去除首尾空格，空元素会被过滤。
     * </p>
     *
     * @param value 逗号分隔的字符串
     * @return 对应类型的字符串集合
     */
    private Collection<String> toCollection(String value) {
        // 空值或空白字符串直接返回空集合
        if (value == null || value.isBlank()) {
            return isSetType() ? Set.of() : List.of();
        }
        // 按逗号拆分字符串
        String[] parts = value.split(DELIMITER);
        List<String> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            // 去除每个元素的首尾空格
            String trimmed = part.trim();
            // 过滤掉空字符串元素
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        // 根据字段声明类型返回对应的集合实现
        return isSetType() ? new LinkedHashSet<>(list) : list;
    }

    /**
     * 判断字段声明类型是否为 Set 类型
     *
     * @return 字段类型可赋值给 Set 时返回 true
     */
    private boolean isSetType() {
        return declaredType != null && Set.class.isAssignableFrom(declaredType);
    }

}
