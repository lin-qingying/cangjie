

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.references.impl.CfirThisReferenceImpl
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirThisReferenceBuilder {
    var source: CjSourceElement? = null
    var boundSymbol: CfirThisOwnerSymbol<*>? = null
    var isImplicit: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var diagnostic: ConeDiagnostic? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirThisReference {
        return CfirThisReferenceImpl(
            source,
            boundSymbol,
            isImplicit,
            diagnostic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildThisReference(init: CfirThisReferenceBuilder.() -> Unit): CfirThisReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirThisReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildThisReferenceCopy(original: CfirThisReference, init: CfirThisReferenceBuilder.() -> Unit): CfirThisReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirThisReferenceBuilder()
    copyBuilder.source = original.source
    copyBuilder.boundSymbol = original.boundSymbol
    copyBuilder.isImplicit = original.isImplicit
    copyBuilder.diagnostic = original.diagnostic
    return copyBuilder.apply(init).build()
}
