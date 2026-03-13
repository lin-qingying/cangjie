

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirInvalidDeclarationImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol

@CfirBuilderDsl
class CfirInvalidDeclarationBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var reason: String

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirInvalidDeclaration {
        return CfirInvalidDeclarationImpl(
            symbol,
            origin,
            annotations,
            moduleData,
            resolvePhase,
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
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes
    copyBuilder.reason = original.reason
    return copyBuilder.apply(init).build()
}
