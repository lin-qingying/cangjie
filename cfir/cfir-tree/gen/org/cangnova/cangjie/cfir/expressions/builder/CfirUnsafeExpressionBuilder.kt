

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirUnsafeExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirUnsafeExpressionImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirUnsafeExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var body: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirUnsafeExpression {
        return CfirUnsafeExpressionImpl(
            source,
            annotations,
            coneTypeOrNull,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildUnsafeExpression(init: CfirUnsafeExpressionBuilder.() -> Unit): CfirUnsafeExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirUnsafeExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildUnsafeExpressionCopy(original: CfirUnsafeExpression, init: CfirUnsafeExpressionBuilder.() -> Unit): CfirUnsafeExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirUnsafeExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
