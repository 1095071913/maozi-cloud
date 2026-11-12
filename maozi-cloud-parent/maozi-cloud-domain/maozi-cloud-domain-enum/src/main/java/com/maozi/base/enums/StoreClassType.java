package com.maozi.base.enums;

import com.maozi.common.enums.BaseEnum;
import lombok.Getter;

/**
 * 存储类型枚举
 * <p>
 * 标识数据的存储介质类型，当前仅支持数据库，可按需扩展缓存、搜索引擎、对象存储等。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public enum StoreClassType implements BaseEnum {

    /** 数据库存储 */
    DB(0,"数据库");

    /**
     * 按整型值和描述构造枚举实例
     *
     * @param value 存储类型的整型值
     * @param desc 存储类型的中文描述
     */
    StoreClassType(Integer value,String desc) {

		this.value = value;

		this.desc = desc;

    }

    /** 枚举的整型值 */
    @Getter
    private final Integer value;

    /** 存储类型的中文描述 */
    @Getter
    private final String desc;

    /**
     * 返回"值.描述"格式的字符串
     *
     * @return 形如 "0.数据库" 的字符串
     */
    @Override
    public String toString() {
        return value + "." + desc;
    }

}
