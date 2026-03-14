package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag

/**
 * 类型推断中的类型变量。
 *
 * 为泛型调用推断创建的临时变量，每个类型变量对应一个声明的类型参数。
 * 约束系统收集该变量的上界/下界约束，最终固定为具体类型。
 *
 * 对齐 K2 TypeVariable（简化为单一类型，去掉 5 种子类）。
 */
data class CfirTypeVariable(
    /** 对应的类型参数符号 */
    val typeParameter: CfirTypeParameterSymbol,
    /** 唯一标识（在同一约束系统内唯一） */
    val freshTypeId: Int,
    /** 类型参数的查找标签 */
    val lookupTag: ConeTypeParameterLookupTag,
) {
    /** 类型参数名称 */
    val name: String get() = lookupTag.name

    /** 收集到的上界约束 */
    val upperBounds: MutableList<ConeCangjieType> = mutableListOf()

    /** 收集到的下界约束 */
    val lowerBounds: MutableList<ConeCangjieType> = mutableListOf()

    /** 已固定的具体类型（null 表示尚未固定） */
    var fixedType: ConeCangjieType? = null

    /** 是否已固定 */
    val isFixed: Boolean get() = fixedType != null

    override fun toString(): String = buildString {
        append("CfirTypeVariable($name#$freshTypeId")
        if (isFixed) append(" = $fixedType")
        else {
            if (lowerBounds.isNotEmpty()) append(" >: ${lowerBounds.joinToString(" | ")}")
            if (upperBounds.isNotEmpty()) append(" <: ${upperBounds.joinToString(" & ")}")
        }
        append(")")
    }
}
