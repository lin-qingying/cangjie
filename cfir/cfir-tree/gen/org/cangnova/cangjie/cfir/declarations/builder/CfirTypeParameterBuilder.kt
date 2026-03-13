

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirTypeParameterBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var name: Name
    val bounds: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypeParameter {
        return CfirTypeParameterImpl(
            symbol,
            origin,
            annotations,
            moduleData,
            resolvePhase,
            attributes,
            name,
            bounds,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameter(init: CfirTypeParameterBuilder.() -> Unit): CfirTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypeParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameterCopy(original: CfirTypeParameter, init: CfirTypeParameterBuilder.() -> Unit): CfirTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypeParameterBuilder()
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes
    copyBuilder.name = original.name
    copyBuilder.bounds.addAll(original.bounds)
    return copyBuilder.apply(init).build()
}
