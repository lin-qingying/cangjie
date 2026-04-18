/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.renderer.ConeAttributeRenderer
import org.cangnova.cangjie.cfir.renderer.ConeIdFullRenderer
import org.cangnova.cangjie.cfir.renderer.ConeTypeRenderer
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.AbbreviatedTypeAttribute
import org.cangnova.cangjie.cfir.types.ConeAttribute
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * **Note**: the signature doesn't contain a name. This check should be done externally.
 */
@CaImplementationDetail
class CfirCallableSignature private constructor(
    private val receiverType: String?,
    private val parameters: List<String>?,
    private val typeParametersCount: Int,
    private val returnType: String,
) {
    fun hasTheSameSignature(declaration: CfirCallableSymbol<*>): Boolean = hasTheSameSignature(declaration.fir)

    fun hasTheSameSignature(declaration: CfirCallableDeclaration): Boolean {
        if ((receiverType == null) != (declaration.receiverParameter == null)) return false
        if (typeParametersCount != declaration.typeParameters.size) return false
        if (parameters?.size != (declaration as? CfirFunction)?.valueParameters?.size) return false

        declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
        if (receiverType != declaration.receiverParameter?.typeRef?.renderType()) return false

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

        if (receiverType != other.receiverType) return false
        if (parameters != other.parameters) return false
        if (typeParametersCount != other.typeParametersCount) return false
        return returnType == other.returnType

    }

    override fun hashCode(): Int {
        var result = receiverType?.hashCode() ?: 0
        result = 31 * result + parameters.hashCode()
        result = 31 * result + typeParametersCount.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    companion object {
        fun createSignature(callableSymbol: CfirCallableSymbol<*>): CfirCallableSignature = createSignature(callableSymbol.fir)

        fun createSignature(callableDeclaration: CfirCallableDeclaration): CfirCallableSignature {
            callableDeclaration.lazyResolveToPhase(CfirResolvePhase.TYPES)

            return CfirCallableSignature(
                receiverType = callableDeclaration.receiverParameter?.typeRef?.renderType(),
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

private fun CfirTypeRef.renderType(builder: StringBuilder = StringBuilder()): String = CfirRenderer(
    builder = builder,
    annotationRenderer = null,
    bodyRenderer = null,
    callArgumentsRenderer = null,
    classMemberRenderer = null,
    contractRenderer = null,
    declarationRenderer = null,
    idRenderer = ConeIdFullRenderer(),
    modifierRenderer = null,
    packageDirectiveRenderer = null,
    propertyAccessorRenderer = null,
    resolvePhaseRenderer = null,
    typeRenderer = ConeTypeRenderer(attributeRenderer = MinimalConeTypeAttributeRenderer),
    callableSignatureRenderer = null,
    errorExpressionRenderer = null,
).renderElementAsString(this)

private object MinimalConeTypeAttributeRenderer : ConeAttributeRenderer() {
    override fun render(attributes: Iterable<ConeAttribute<*>>): String =
        attributes.filter { it.isImportant }.let(ToString::render)

    private val ConeAttribute<*>.isImportant get() = this is AbbreviatedTypeAttribute
}
