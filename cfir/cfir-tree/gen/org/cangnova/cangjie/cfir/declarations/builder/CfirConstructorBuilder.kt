

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirConstructorImpl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef

@CfirBuilderDsl
class CfirConstructorBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: CfirTypeRef
    val valueParameters: MutableList<CfirValueParameter> = mutableListOf()
    var body: CfirBlock? = null
    var isPrimary: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirConstructor {
        return CfirConstructorImpl(
            symbol,
            origin,
            annotations,
            moduleData,
            resolvePhase,
            attributes,
            status,
            typeParameters,
            returnTypeRef,
            valueParameters,
            body,
            isPrimary,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructor(init: CfirConstructorBuilder.() -> Unit): CfirConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirConstructorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructorCopy(original: CfirConstructor, init: CfirConstructorBuilder.() -> Unit): CfirConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirConstructorBuilder()
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.isPrimary = original.isPrimary
    return copyBuilder.apply(init).build()
}
