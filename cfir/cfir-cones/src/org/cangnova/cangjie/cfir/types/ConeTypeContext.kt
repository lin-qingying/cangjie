package org.cangnova.cangjie.cfir.types

/**
 * 类型上下文接口，提供类型关系查询所需的最小语义上下文。
 *
 * 生产环境由 [CfirTypeCheckerContext] 实现（依赖 CfirSession），
 * 测试环境可提供轻量级实现。
 */
interface ConeTypeContext {
    /** 查询给定类型的直接超类型集合 */
    fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType>

    /** 判断两个类型是否具有相同的类型构造器 */
    fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean
}
