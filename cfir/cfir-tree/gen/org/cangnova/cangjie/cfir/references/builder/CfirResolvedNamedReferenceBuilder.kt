

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedNamedReferenceImpl
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirResolvedNamedReferenceBuilder {
    var source: CjSourceElement? = null
    lateinit var name: Name
    lateinit var resolvedSymbol: CfirBasedSymbol<*>

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirResolvedNamedReference {
        return CfirResolvedNamedReferenceImpl(
            source,
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
    copyBuilder.source = original.source
    copyBuilder.name = original.name
    copyBuilder.resolvedSymbol = original.resolvedSymbol
    return copyBuilder.apply(init).build()
}
