package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker
import org.cangnova.cangjie.cfir.types.ConeTypeContext

/**
 * 类型检查工具类。
 * 提供子类型判断和类型渲染能力，供各类 checker 复用。
 * 这里使用 [BasicConeTypeContext]，避免形成 `checkers -> resolve` 的循环依赖。
 */
object CfirTypeCheckUtils {
    private val subtypeChecker = ConeSubtypeChecker(BasicConeTypeContext)

    /** 判断 [subType] 是否为 [superType] 的子类型。 */
    fun isSubtypeOf(subType: ConeCangjieType, superType: ConeCangjieType): Boolean =
        subtypeChecker.isSubtypeOf(subType, superType)

    /** 将类型渲染为可读字符串。 */
    fun renderType(type: ConeCangjieType): String = type.toString()

    /**
     * 基础类型上下文。
     * 仅支持内建规则，如 IdealType / Nothing / Any / 相等 / 原始类型，
     * 不提供超类型图遍历；类继承关系由后续增强补齐。
     */
    private object BasicConeTypeContext : ConeTypeContext {
        override fun supertypes(type: ConeCangjieType): Collection<ConeCangjieType> = emptyList()
        override fun isSameTypeConstructor(a: ConeCangjieType, b: ConeCangjieType): Boolean = a == b
    }
}

