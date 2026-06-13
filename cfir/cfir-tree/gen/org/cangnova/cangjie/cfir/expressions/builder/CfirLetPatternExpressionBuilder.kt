

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLetPatternExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirLetPatternExpressionImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirLetPatternExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var initializer: CfirExpression
    lateinit var pattern: CfirPattern

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirLetPatternExpression {
        return CfirLetPatternExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            initializer,
            pattern,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildLetPatternExpression(init: CfirLetPatternExpressionBuilder.() -> Unit): CfirLetPatternExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirLetPatternExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLetPatternExpressionCopy(original: CfirLetPatternExpression, init: CfirLetPatternExpressionBuilder.() -> Unit): CfirLetPatternExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirLetPatternExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.initializer = original.initializer
    copyBuilder.pattern = original.pattern
    return copyBuilder.apply(init).build()
}
