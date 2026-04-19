/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.render.ConeAttributeRenderer
import org.cangnova.cangjie.cfir.render.ConeFullyQualifiedIdRenderer
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeAttribute
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * **Note**: the signature doesn't contain a name. This check should be done externally.
 */
@CaImplementationDetail
class CfirCallableSignature private constructor(
    private val parameters: List<String>?,
    private val typeParametersCount: Int,
    private val returnType: String,
) {
    fun hasTheSameSignature(declaration: CfirCallableSymbol<*>): Boolean = hasTheSameSignature(declaration.cfir)

    fun hasTheSameSignature(declaration: CfirCallableDeclaration): Boolean {
        if (typeParametersCount != declaration.typeParameters.size) return false
        if (parameters?.size != (declaration as? CfirFunction)?.valueParameters?.size) return false

        declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)

        if (declaration is CfirFunction) {
            requireNotNull(parameters)
            for ((index, parameter) in declaration.valueParameters.withIndex()) {
                if (parameters[index] != parameter.returnTypeRef.renderType()) return false
            }
        }

        return returnType == declaration.symbol.resolvedReturnTypeRef.renderType()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CfirCallableSignature) return false

        if (parameters != other.parameters) return false
        if (typeParametersCount != other.typeParametersCount) return false
        return returnType == other.returnType

    }

    override fun hashCode(): Int {
        var result = parameters.hashCode()
        result = 31 * result + typeParametersCount.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    companion object {
        fun createSignature(callableSymbol: CfirCallableSymbol<*>): CfirCallableSignature = createSignature(callableSymbol.cfir)

        fun createSignature(callableDeclaration: CfirCallableDeclaration): CfirCallableSignature {
            callableDeclaration.lazyResolveToPhase(CfirResolvePhase.TYPES)

            return CfirCallableSignature(
                parameters = if (callableDeclaration is CfirFunction) {
                    callableDeclaration.valueParameters.map { it.returnTypeRef.renderType() }
                } else {
                    null
                },
                typeParametersCount = callableDeclaration.typeParameters.size,
                returnType = callableDeclaration.symbol.resolvedReturnTypeRef.renderType(),
            )
        }
    }
}

private fun CfirTypeRef.renderType(builder: StringBuilder = StringBuilder()): String {
    val typeRenderer = ConeTypeRenderer(attributeRenderer = MinimalConeTypeAttributeRenderer).apply {
        idRenderer = ConeFullyQualifiedIdRenderer()
    }
    return CfirRenderer(
        builder = builder,
        annotationRenderer = null,
        declarationRenderer = null,
        packageDirectiveRenderer = null,
        resolvePhaseRenderer = null,
        errorExpressionRenderer = null,
        typeRenderer = typeRenderer,
        callableSignatureRenderer = null,
        modifierRenderer = null,
        inlineExpressionRenderer = null,
        patternRenderer = null,
    ).renderElementAsString(this)
}

private object MinimalConeTypeAttributeRenderer : ConeAttributeRenderer() {
    override fun render(attributes: Iterable<ConeAttribute<*>>): String =
        attributes.filter { it.isImportant }.let(ToString::render)

    private val ConeAttribute<*>.isImportant get() = false
}
