

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeAliasImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirTypeAliasBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var name: Name
    lateinit var expandedTypeRef: CfirTypeRef

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypeAlias {
        return CfirTypeAliasImpl(
            symbol,
            origin,
            annotations,
            moduleData,
            resolvePhase,
            attributes,
            status,
            typeParameters,
            name,
            expandedTypeRef,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAlias(init: CfirTypeAliasBuilder.() -> Unit): CfirTypeAlias {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypeAliasBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAliasCopy(original: CfirTypeAlias, init: CfirTypeAliasBuilder.() -> Unit): CfirTypeAlias {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypeAliasBuilder()
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.name = original.name
    copyBuilder.expandedTypeRef = original.expandedTypeRef
    return copyBuilder.apply(init).build()
}
