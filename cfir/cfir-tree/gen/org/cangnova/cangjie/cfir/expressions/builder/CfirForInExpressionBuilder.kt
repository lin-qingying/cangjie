

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
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirForInExpressionImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirForInExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var pattern: CfirPattern
    lateinit var iterable: CfirExpression
    var patternGuard: CfirExpression? = null
    lateinit var body: CfirBlock
    lateinit var iterationKind: CfirSourceForInKind

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirForInExpression {
        return CfirForInExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            pattern,
            iterable,
            patternGuard,
            body,
            iterationKind,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildForInExpression(init: CfirForInExpressionBuilder.() -> Unit): CfirForInExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirForInExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildForInExpressionCopy(original: CfirForInExpression, init: CfirForInExpressionBuilder.() -> Unit): CfirForInExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirForInExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.iterable = original.iterable
    copyBuilder.patternGuard = original.patternGuard
    copyBuilder.body = original.body
    copyBuilder.iterationKind = original.iterationKind
    return copyBuilder.apply(init).build()
}
