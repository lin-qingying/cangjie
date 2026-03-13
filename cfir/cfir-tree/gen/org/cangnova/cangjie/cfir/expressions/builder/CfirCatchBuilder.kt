

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.impl.CfirCatchImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirCatchBuilder {
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var parameter: CfirValueParameter
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirCatch {
        return CfirCatchImpl(
            coneTypeOrNull,
            parameter,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCatch(init: CfirCatchBuilder.() -> Unit): CfirCatch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirCatchBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildCatchCopy(original: CfirCatch, init: CfirCatchBuilder.() -> Unit): CfirCatch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirCatchBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.parameter = original.parameter
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
