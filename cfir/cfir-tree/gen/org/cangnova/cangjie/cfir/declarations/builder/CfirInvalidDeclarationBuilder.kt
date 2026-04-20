

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirInvalidDeclarationImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirInvalidDeclarationBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var symbol: CfirBasedSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirInvalidDeclaration {
        return CfirInvalidDeclarationImpl(
            source,
            moduleData,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            symbol,
            origin,
            attributes,
            reason,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildInvalidDeclaration(init: CfirInvalidDeclarationBuilder.() -> Unit): CfirInvalidDeclaration {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirInvalidDeclarationBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildInvalidDeclarationCopy(original: CfirInvalidDeclaration, init: CfirInvalidDeclarationBuilder.() -> Unit): CfirInvalidDeclaration {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirInvalidDeclarationBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
