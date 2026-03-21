package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag

/**
 * 类型推断中的类型变量。
 * 它是为泛型调用推断临时创建的变量，每个变量对应一个声明上的类型参数。
 * 约束系统会收集它的上下界约束，最终再固定为具体类型。
 * 对齐 K2 `TypeVariable`，但做了简化。
 */
data class CfirTypeVariable(
    /** 对应的类型参数符号。 */
    val typeParameter: CfirTypeParameterSymbol,
    /** 唯一标识，在同一约束系统内唯一。 */
    val freshTypeId: Int,
    /** 类型参数对应的查找标签。 */
    val lookupTag: ConeTypeParameterLookupTag,
) {
    /** 类型参数名称。 */
    val name: String get() = lookupTag.name

    /** 收集到的上界约束。 */
    val upperBounds: MutableList<ConeCangJieType> = mutableListOf()

    /** 收集到的下界约束。 */
    val lowerBounds: MutableList<ConeCangJieType> = mutableListOf()

    /** 已固定的具体类型；`null` 表示尚未固定。 */
    var fixedType: ConeCangJieType? = null

    /** 是否已经固定。 */
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

