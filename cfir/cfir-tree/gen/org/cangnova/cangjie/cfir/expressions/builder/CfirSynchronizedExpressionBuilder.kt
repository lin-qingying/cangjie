

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSynchronizedExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirSynchronizedExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirSynchronizedExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var monitor: CfirExpression
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirSynchronizedExpression {
        return CfirSynchronizedExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            monitor,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSynchronizedExpression(init: CfirSynchronizedExpressionBuilder.() -> Unit): CfirSynchronizedExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirSynchronizedExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildSynchronizedExpressionCopy(original: CfirSynchronizedExpression, init: CfirSynchronizedExpressionBuilder.() -> Unit): CfirSynchronizedExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirSynchronizedExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.monitor = original.monitor
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
