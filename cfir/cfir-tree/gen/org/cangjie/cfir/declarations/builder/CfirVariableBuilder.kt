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
import org.cangjie.cfir.declarations.impl.CfirVariableImpl
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirVariableBuilder {
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
    var initializer: CfirExpression? = null
    var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirVariable {
        return CfirVariableImpl(
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
            initializer,
            isVar,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVariable(init: CfirVariableBuilder.() -> Unit): CfirVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirVariableBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildVariableCopy(original: CfirVariable, init: CfirVariableBuilder.() -> Unit): CfirVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirVariableBuilder()
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
    copyBuilder.initializer = original.initializer
    copyBuilder.isVar = original.isVar
    return copyBuilder.apply(init).build()
}
