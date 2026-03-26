

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirFunctionCallBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var calleeReference: CfirReference
    var explicitReceiver: CfirExpression? = null
    var dispatchReceiver: CfirExpression? = null
    val arguments: MutableList<CfirExpression> = mutableListOf()
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()
    lateinit var origin: CfirFunctionCallOrigin

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirFunctionCall {
        return CfirFunctionCallImpl(
            source,
            annotations,
            coneTypeOrNull,
            calleeReference,
            explicitReceiver,
            dispatchReceiver,
            arguments,
            typeArguments,
            origin,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCall(init: CfirFunctionCallBuilder.() -> Unit): CfirFunctionCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirFunctionCallBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCallCopy(original: CfirFunctionCall, init: CfirFunctionCallBuilder.() -> Unit): CfirFunctionCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirFunctionCallBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.explicitReceiver = original.explicitReceiver
    copyBuilder.dispatchReceiver = original.dispatchReceiver
    copyBuilder.arguments.addAll(original.arguments)
    copyBuilder.typeArguments.addAll(original.typeArguments)
    copyBuilder.origin = original.origin
    return copyBuilder.apply(init).build()
}
