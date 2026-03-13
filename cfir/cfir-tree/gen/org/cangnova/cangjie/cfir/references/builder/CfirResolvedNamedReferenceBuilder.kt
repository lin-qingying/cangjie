

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedNamedReferenceImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirResolvedNamedReferenceBuilder {
    lateinit var name: Name
    lateinit var resolvedSymbol: CfirSymbol<*>

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirResolvedNamedReference {
        return CfirResolvedNamedReferenceImpl(
            name,
            resolvedSymbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedNamedReference(init: CfirResolvedNamedReferenceBuilder.() -> Unit): CfirResolvedNamedReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedNamedReferenceBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedNamedReferenceCopy(original: CfirResolvedNamedReference, init: CfirResolvedNamedReferenceBuilder.() -> Unit): CfirResolvedNamedReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirResolvedNamedReferenceBuilder()
    copyBuilder.name = original.name
    copyBuilder.resolvedSymbol = original.resolvedSymbol
    return copyBuilder.apply(init).build()
}
