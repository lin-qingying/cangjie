

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirMacroExpansionDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.impl.CfirMacroExpansionDeclarationImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirMacroExpansionDeclarationBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    var contentSource: CfirContentSource = CfirContentSource.Owned
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var symbol: CfirBasedSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes

    fun build(): CfirMacroExpansionDeclaration {
        return CfirMacroExpansionDeclarationImpl(
            source,
            moduleData,
            resolvePhase,
            contentSource,
            annotations.toMutableOrEmpty(),
            symbol,
            origin,
            attributes,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildMacroExpansionDeclaration(init: CfirMacroExpansionDeclarationBuilder.() -> Unit): CfirMacroExpansionDeclaration {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirMacroExpansionDeclarationBuilder().apply(init).build()
}
