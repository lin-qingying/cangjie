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
import org.cangjie.cfir.declarations.impl.CfirInvalidDeclarationImpl
import org.cangjie.cfir.symbols.CfirSymbol

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
