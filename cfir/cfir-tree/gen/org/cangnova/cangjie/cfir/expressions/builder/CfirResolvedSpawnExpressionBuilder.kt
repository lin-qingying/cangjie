

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionArgument
import org.cangnova.cangjie.cfir.expressions.CfirResolvedSpawnExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirResolvedSpawnExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirResolvedSpawnExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    var argument: CfirFunctionArgument? = null
    lateinit var future: CfirFieldVariable

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirResolvedSpawnExpression {
        return CfirResolvedSpawnExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            argument,
            future,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedSpawnExpression(init: CfirResolvedSpawnExpressionBuilder.() -> Unit): CfirResolvedSpawnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedSpawnExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedSpawnExpressionCopy(original: CfirResolvedSpawnExpression, init: CfirResolvedSpawnExpressionBuilder.() -> Unit): CfirResolvedSpawnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirResolvedSpawnExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.argument = original.argument
    copyBuilder.future = original.future
    return copyBuilder.apply(init).build()
}
