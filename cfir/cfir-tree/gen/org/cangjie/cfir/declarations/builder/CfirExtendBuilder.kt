

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.declarations.impl.CfirExtendImpl
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.types.CfirTypeRef

@CfirBuilderDsl
class CfirExtendBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var extendedTypeRef: CfirTypeRef
    val superTypeRefs: MutableList<CfirTypeRef> = mutableListOf()
    val declarations: MutableList<CfirDeclaration> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirExtend {
        return CfirExtendImpl(
            symbol,
            origin,
            annotations,
            moduleData,
            resolvePhase,
            attributes,
            status,
            typeParameters,
            extendedTypeRef,
            superTypeRefs,
            declarations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildExtend(init: CfirExtendBuilder.() -> Unit): CfirExtend {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirExtendBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildExtendCopy(original: CfirExtend, init: CfirExtendBuilder.() -> Unit): CfirExtend {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirExtendBuilder()
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.extendedTypeRef = original.extendedTypeRef
    copyBuilder.superTypeRefs.addAll(original.superTypeRefs)
    copyBuilder.declarations.addAll(original.declarations)
    return copyBuilder.apply(init).build()
}
