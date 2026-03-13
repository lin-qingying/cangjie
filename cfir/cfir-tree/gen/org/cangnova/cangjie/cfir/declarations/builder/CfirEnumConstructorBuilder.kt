

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirEnumConstructorImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirEnumConstructorBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirEnumConstructor {
        return CfirEnumConstructorImpl(
            symbol,
            origin,
            annotations,
            moduleData,
            resolvePhase,
            attributes,
            status,
            typeParameters,
            returnTypeRef,
            name,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumConstructor(init: CfirEnumConstructorBuilder.() -> Unit): CfirEnumConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirEnumConstructorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumConstructorCopy(original: CfirEnumConstructor, init: CfirEnumConstructorBuilder.() -> Unit): CfirEnumConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirEnumConstructorBuilder()
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
