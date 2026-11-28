

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
import org.cangnova.cangjie.cfir.expressions.CfirWildcardExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirWildcardExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirWildcardExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirWildcardExpression {
        return CfirWildcardExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWildcardExpression(init: CfirWildcardExpressionBuilder.() -> Unit = {}): CfirWildcardExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirWildcardExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildWildcardExpressionCopy(original: CfirWildcardExpression, init: CfirWildcardExpressionBuilder.() -> Unit = {}): CfirWildcardExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirWildcardExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    return copyBuilder.apply(init).build()
}
