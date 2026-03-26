

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedErrorReferenceImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirResolvedErrorReferenceBuilder {
    var source: CjSourceElement? = null
    lateinit var name: Name
    lateinit var resolvedSymbol: CfirSymbol<*>
    lateinit var diagnostic: ConeDiagnostic

    fun build(): CfirResolvedErrorReference {
        return CfirResolvedErrorReferenceImpl(
            source,
            name,
            resolvedSymbol,
            diagnostic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedErrorReference(init: CfirResolvedErrorReferenceBuilder.() -> Unit): CfirResolvedErrorReference {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedErrorReferenceBuilder().apply(init).build()
}
