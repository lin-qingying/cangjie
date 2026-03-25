

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirErrorExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirErrorExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var diagnostic: ConeDiagnostic
    var expression: CfirExpression? = null
    var nonExpressionElement: CfirElement? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirErrorExpression {
        return CfirErrorExpressionImpl(
            source,
            annotations,
            diagnostic,
            expression,
            nonExpressionElement,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorExpression(init: CfirErrorExpressionBuilder.() -> Unit): CfirErrorExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorExpressionCopy(original: CfirErrorExpression, init: CfirErrorExpressionBuilder.() -> Unit): CfirErrorExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirErrorExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.diagnostic = original.diagnostic
    copyBuilder.expression = original.expression
    copyBuilder.nonExpressionElement = original.nonExpressionElement
    return copyBuilder.apply(init).build()
}
