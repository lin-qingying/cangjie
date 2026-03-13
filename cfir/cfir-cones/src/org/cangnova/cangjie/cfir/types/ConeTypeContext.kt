package org.cangnova.cangjie.cfir.types

/**
 * 类型系统操作抽象接口，解耦子类型判定算法与符号系统。
 *
 * 子类型检查器通过此接口获取类型的超类型信息和构造器相等性，
 * 而无需直接依赖具体的符号查询实现。
 *
 * 参考 K2 TypeSystemContext。
 */
interface ConeTypeContext {

    /** 获取指定类型的直接超类型列表 */
    fun supertypes(type: ConeCangjieType): Collection<ConeCangjieType>

    /** 判断两个类型是否拥有相同的类型构造器（忽略类型参数） */
    fun isSameTypeConstructor(a: ConeCangjieType, b: ConeCangjieType): Boolean
}
