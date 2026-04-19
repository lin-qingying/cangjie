

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
open class CfirFunctionCallBuilder : CfirAbstractFunctionCallBuilder {
    override var source: CjSourceElement? = null
    override val annotations: MutableList<CfirAnnotation> = mutableListOf()
    override var coneTypeOrNull: ConeCangJieType? = null
    override lateinit var calleeReference: CfirReference
    override var dispatchReceiver: CfirExpression? = null
    override var explicitReceiver: CfirExpression? = null
    override val typeArguments: MutableList<CfirTypeRef> = mutableListOf()
    override var argumentList: CfirArgumentList = CfirEmptyArgumentList
    override var origin: CfirFunctionCallOrigin = CfirFunctionCallOrigin.Regular
    override var hasTrailingLambda: Boolean = false

    @OptIn(CfirImplementationDetail::class)
    override fun build(): CfirFunctionCall {
        return CfirFunctionCallImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            calleeReference,
            dispatchReceiver,
            explicitReceiver,
            typeArguments.toMutableOrEmpty(),
            argumentList,
            origin,
            hasTrailingLambda,
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
    copyBuilder.dispatchReceiver = original.dispatchReceiver
    copyBuilder.explicitReceiver = original.explicitReceiver
    copyBuilder.typeArguments.addAll(original.typeArguments)
    copyBuilder.argumentList = original.argumentList
    copyBuilder.origin = original.origin
    copyBuilder.hasTrailingLambda = original.hasTrailingLambda
    return copyBuilder.apply(init).build()
}
