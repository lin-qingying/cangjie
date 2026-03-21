

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirRangeExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirRangeExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var start: CfirExpression
    lateinit var end: CfirExpression
    var isInclusive: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirRangeExpression {
        return CfirRangeExpressionImpl(
            source,
            annotations,
            coneTypeOrNull,
            start,
            end,
            isInclusive,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildRangeExpression(init: CfirRangeExpressionBuilder.() -> Unit): CfirRangeExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirRangeExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildRangeExpressionCopy(original: CfirRangeExpression, init: CfirRangeExpressionBuilder.() -> Unit): CfirRangeExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirRangeExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.start = original.start
    copyBuilder.end = original.end
    copyBuilder.isInclusive = original.isInclusive
    return copyBuilder.apply(init).build()
}
