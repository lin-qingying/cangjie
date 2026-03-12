/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.declarations.impl.CfirFinalizerImpl
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.types.CfirTypeRef

@CfirBuilderDsl
class CfirFinalizerBuilder {
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

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirFinalizer {
        return CfirFinalizerImpl(
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
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFinalizer(init: CfirFinalizerBuilder.() -> Unit): CfirFinalizer {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirFinalizerBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFinalizerCopy(original: CfirFinalizer, init: CfirFinalizerBuilder.() -> Unit): CfirFinalizer {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirFinalizerBuilder()
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
    return copyBuilder.apply(init).build()
}
