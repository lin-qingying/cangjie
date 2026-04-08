

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
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirNamedAccessExpressionImpl
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNamedAccessExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var calleeReference: CfirReference
    var dispatchReceiver: CfirExpression? = null
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()
    var explicitReceiver: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirNamedAccessExpression {
        return CfirNamedAccessExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            calleeReference,
            dispatchReceiver,
            typeArguments.toMutableOrEmpty(),
            explicitReceiver,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedAccessExpression(init: CfirNamedAccessExpressionBuilder.() -> Unit): CfirNamedAccessExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNamedAccessExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedAccessExpressionCopy(original: CfirNamedAccessExpression, init: CfirNamedAccessExpressionBuilder.() -> Unit): CfirNamedAccessExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNamedAccessExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.dispatchReceiver = original.dispatchReceiver
    copyBuilder.typeArguments.addAll(original.typeArguments)
    copyBuilder.explicitReceiver = original.explicitReceiver
    return copyBuilder.apply(init).build()
}
