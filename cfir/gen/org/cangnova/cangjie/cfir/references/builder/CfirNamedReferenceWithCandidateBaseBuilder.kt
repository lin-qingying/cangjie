

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.references.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.impl.CfirNamedReferenceWithCandidateBaseImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNamedReferenceWithCandidateBaseBuilder {
    var source: CjSourceElement? = null
    lateinit var name: Name
    lateinit var candidateSymbol: CfirSymbol<*>

    fun build(): CfirNamedReferenceWithCandidateBase {
        return CfirNamedReferenceWithCandidateBaseImpl(
            source,
            name,
            candidateSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedReferenceWithCandidateBase(init: CfirNamedReferenceWithCandidateBaseBuilder.() -> Unit): CfirNamedReferenceWithCandidateBase {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNamedReferenceWithCandidateBaseBuilder().apply(init).build()
}
