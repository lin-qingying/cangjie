package org.cangnova.cangjie.analysis.api.cfir.signatures

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirMapBackedSubstitutor
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.signatures.CaValueParameterSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType

internal open class CaCfirSubstitutedSignatureImpl<out S : CaCallableSymbol>(
    override val symbol: S,
    typeParameters: List<CaTypeParameterSymbol>,
    valueParameters: List<CaValueParameterSignature>,
    returnType: CaType?,
    receiverType: CaType?,
    annotations: List<CaAnnotation>,
    analysisSession: CaCfirSession,
    token: org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken,
) : CaCfirSignatureImpl<S>(
    symbol = symbol,
    typeParameters = typeParameters,
    valueParameters = valueParameters,
    returnType = returnType,
    receiverType = receiverType,
    annotations = annotations,
    analysisSession = analysisSession,
    token = token,
)

internal class CaCfirSubstitutedFunctionSignatureImpl<out S : CaFunctionSymbol>(
    symbol: S,
    typeParameters: List<CaTypeParameterSymbol>,
    valueParameters: List<CaValueParameterSignature>,
    returnType: CaType?,
    receiverType: CaType?,
    annotations: List<CaAnnotation>,
    analysisSession: CaCfirSession,
    token: org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken,
) : CaCfirSubstitutedSignatureImpl<S>(
    symbol = symbol,
    typeParameters = typeParameters,
    valueParameters = valueParameters,
    returnType = returnType,
    receiverType = receiverType,
    annotations = annotations,
    analysisSession = analysisSession,
    token = token,
), CaFunctionSignature<S>

internal class CaCfirSubstitutedVariableSignatureImpl<out S : CaVariableSymbol>(
    symbol: S,
    typeParameters: List<CaTypeParameterSymbol>,
    valueParameters: List<CaValueParameterSignature>,
    returnType: CaType?,
    receiverType: CaType?,
    annotations: List<CaAnnotation>,
    analysisSession: CaCfirSession,
    token: org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken,
) : CaCfirSubstitutedSignatureImpl<S>(
    symbol = symbol,
    typeParameters = typeParameters,
    valueParameters = valueParameters,
    returnType = returnType,
    receiverType = receiverType,
    annotations = annotations,
    analysisSession = analysisSession,
    token = token,
), CaVariableSignature<S>

internal fun <S : CaCallableSymbol> CaCfirSession.substituteSignature(
    signature: CaSignature<S>,
    substitutor: CaSubstitutor,
): CaSignature<S> {
    if (substitutor is CaSubstitutor.Empty) return signature

    val cfirSubstitutor = when (substitutor) {
        is CaCfirMapBackedSubstitutor -> substitutor
        else -> error("仅支持使用 CFIR substitutor 实例化签名：${substitutor::class.simpleName}")
    }

    val cacheKey = CaCfirSubstitutedSignatureCacheKey(signature, cfirSubstitutor.mappings)
    @Suppress("UNCHECKED_CAST")
    return getOrCreateSubstitutedSignature(cacheKey) {
        buildSubstitutedSignature(signature, cfirSubstitutor)
    } as CaSignature<S>
}

private fun <S : CaCallableSymbol> CaCfirSession.buildSubstitutedSignature(
    signature: CaSignature<S>,
    substitutor: CaCfirMapBackedSubstitutor,
): CaSignature<S> {
    val substitutedValueParameters = signature.valueParameters.map { parameter ->
        CaCfirValueParameterSignatureImpl(
            name = parameter.name,
            type = parameter.type?.let(substitutor::substitute),
            annotations = parameter.annotations,
            token = token,
        )
    }

    @Suppress("UNCHECKED_CAST")
    return when (val symbol = signature.symbol) {
        is CaFunctionSymbol -> CaCfirSubstitutedFunctionSignatureImpl(
            symbol = symbol,
            typeParameters = signature.typeParameters,
            valueParameters = substitutedValueParameters,
            returnType = signature.returnType?.let(substitutor::substitute),
            receiverType = signature.receiverType?.let(substitutor::substitute),
            annotations = signature.annotations,
            analysisSession = this,
            token = token,
        ) as CaSignature<S>

        is CaVariableSymbol -> CaCfirSubstitutedVariableSignatureImpl(
            symbol = symbol,
            typeParameters = signature.typeParameters,
            valueParameters = substitutedValueParameters,
            returnType = signature.returnType?.let(substitutor::substitute),
            receiverType = signature.receiverType?.let(substitutor::substitute),
            annotations = signature.annotations,
            analysisSession = this,
            token = token,
        ) as CaSignature<S>

        else -> CaCfirSubstitutedSignatureImpl(
            symbol = symbol,
            typeParameters = signature.typeParameters,
            valueParameters = substitutedValueParameters,
            returnType = signature.returnType?.let(substitutor::substitute),
            receiverType = signature.receiverType?.let(substitutor::substitute),
            annotations = signature.annotations,
            analysisSession = this,
            token = token,
        )
    }
}
