

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirSuperReceiverExpressionImpl
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirSuperReceiverExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var dispatchReceiver: CfirExpression? = null
    var explicitReceiver: CfirExpression? = null
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()
    lateinit var calleeReference: CfirSuperReference

    fun build(): CfirSuperReceiverExpression {
        return CfirSuperReceiverExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            dispatchReceiver,
            explicitReceiver,
            typeArguments.toMutableOrEmpty(),
            calleeReference,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSuperReceiverExpression(init: CfirSuperReceiverExpressionBuilder.() -> Unit): CfirSuperReceiverExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirSuperReceiverExpressionBuilder().apply(init).build()
}
