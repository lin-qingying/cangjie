

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
import org.cangnova.cangjie.cfir.expressions.CfirLetPatternDestructorExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirLetPatternDestructorExpressionImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirLetPatternDestructorExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var initializer: CfirExpression
    val patterns: MutableList<CfirPattern> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLetPatternDestructorExpression {
        return CfirLetPatternDestructorExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            initializer,
            patterns,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLetPatternDestructorExpression(init: CfirLetPatternDestructorExpressionBuilder.() -> Unit): CfirLetPatternDestructorExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLetPatternDestructorExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLetPatternDestructorExpressionCopy(original: CfirLetPatternDestructorExpression, init: CfirLetPatternDestructorExpressionBuilder.() -> Unit): CfirLetPatternDestructorExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirLetPatternDestructorExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.initializer = original.initializer
    copyBuilder.patterns.addAll(original.patterns)
    return copyBuilder.apply(init).build()
}
