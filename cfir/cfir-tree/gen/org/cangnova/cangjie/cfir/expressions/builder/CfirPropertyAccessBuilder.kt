

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.impl.CfirPropertyAccessImpl
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirPropertyAccessBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var calleeReference: CfirReference
    var explicitReceiver: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirPropertyAccess {
        return CfirPropertyAccessImpl(
            source,
            annotations,
            coneTypeOrNull,
            calleeReference,
            explicitReceiver,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccess(init: CfirPropertyAccessBuilder.() -> Unit): CfirPropertyAccess {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirPropertyAccessBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessCopy(original: CfirPropertyAccess, init: CfirPropertyAccessBuilder.() -> Unit): CfirPropertyAccess {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirPropertyAccessBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.explicitReceiver = original.explicitReceiver
    return copyBuilder.apply(init).build()
}
