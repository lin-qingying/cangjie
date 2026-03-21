

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.expressions.impl.CfirQualifiedAccessImpl
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirQualifiedAccessBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var calleeReference: CfirReference
    var explicitReceiver: CfirExpression? = null
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirQualifiedAccess {
        return CfirQualifiedAccessImpl(
            source,
            annotations,
            coneTypeOrNull,
            calleeReference,
            explicitReceiver,
            typeArguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifiedAccess(init: CfirQualifiedAccessBuilder.() -> Unit): CfirQualifiedAccess {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirQualifiedAccessBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifiedAccessCopy(original: CfirQualifiedAccess, init: CfirQualifiedAccessBuilder.() -> Unit): CfirQualifiedAccess {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirQualifiedAccessBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.explicitReceiver = original.explicitReceiver
    copyBuilder.typeArguments.addAll(original.typeArguments)
    return copyBuilder.apply(init).build()
}
