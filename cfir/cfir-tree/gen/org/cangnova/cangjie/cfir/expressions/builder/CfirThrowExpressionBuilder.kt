

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirThrowExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirThrowExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var exception: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirThrowExpression {
        return CfirThrowExpressionImpl(
            source,
            annotations,
            coneTypeOrNull,
            exception,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildThrowExpression(init: CfirThrowExpressionBuilder.() -> Unit): CfirThrowExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirThrowExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildThrowExpressionCopy(original: CfirThrowExpression, init: CfirThrowExpressionBuilder.() -> Unit): CfirThrowExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirThrowExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.exception = original.exception
    return copyBuilder.apply(init).build()
}
