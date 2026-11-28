

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.impl.CfirLiteralExpressionImpl
import org.cangnova.cangjie.cfir.source.CfirSourceStringKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirLiteralExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var kind: CfirLiteralKind
    var value: Any? = null
    var stringKind: CfirSourceStringKind = CfirSourceStringKind.NORMAL

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLiteralExpression {
        return CfirLiteralExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            kind,
            value,
            stringKind,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLiteralExpression(init: CfirLiteralExpressionBuilder.() -> Unit): CfirLiteralExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLiteralExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLiteralExpressionCopy(original: CfirLiteralExpression, init: CfirLiteralExpressionBuilder.() -> Unit): CfirLiteralExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirLiteralExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.kind = original.kind
    copyBuilder.value = original.value
    copyBuilder.stringKind = original.stringKind
    return copyBuilder.apply(init).build()
}
