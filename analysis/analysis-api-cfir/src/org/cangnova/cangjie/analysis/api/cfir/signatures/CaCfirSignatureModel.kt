package org.cangnova.cangjie.analysis.api.cfir.signatures

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolSupport
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.signatures.CaValueParameterSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration

/**
 * value parameter 的公开 use-site 签名。
 *
 * 这里只承载稳定语义，不混入 PSI 文本或后端内部节点。
 */
internal class CaCfirValueParameterSignatureImpl(
    override val name: Name?,
    override val type: CaType?,
    override val annotations: List<CaAnnotation>,
    override val token: CaLifetimeToken,
) : CaValueParameterSignature

/**
 * CFIR 侧的通用 callable use-site 签名实现。
 */
internal open class CaCfirSignatureImpl<out S : CaCallableSymbol>(
    override val symbol: S,
    override val typeParameters: List<CaTypeParameterSymbol>,
    override val valueParameters: List<CaValueParameterSignature>,
    override val returnType: CaType?,
    override val receiverType: CaType?,
    override val annotations: List<CaAnnotation>,
    private val analysisSession: CaCfirSession,
    override val token: CaLifetimeToken,
) : CaSignature<S> {
    override fun substitute(substitutor: CaSubstitutor): CaSignature<S> {
        return analysisSession.substituteSignature(this, substitutor)
    }
}

internal class CaCfirFunctionSignatureImpl<out S : CaFunctionSymbol>(
    symbol: S,
    typeParameters: List<CaTypeParameterSymbol>,
    valueParameters: List<CaValueParameterSignature>,
    returnType: CaType?,
    receiverType: CaType?,
    annotations: List<CaAnnotation>,
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
) : CaCfirSignatureImpl<S>(
    symbol = symbol,
    typeParameters = typeParameters,
    valueParameters = valueParameters,
    returnType = returnType,
    receiverType = receiverType,
    annotations = annotations,
    analysisSession = analysisSession,
    token = token,
), CaFunctionSignature<S>

internal class CaCfirVariableSignatureImpl<out S : CaVariableSymbol>(
    symbol: S,
    typeParameters: List<CaTypeParameterSymbol>,
    valueParameters: List<CaValueParameterSignature>,
    returnType: CaType?,
    receiverType: CaType?,
    annotations: List<CaAnnotation>,
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
) : CaCfirSignatureImpl<S>(
    symbol = symbol,
    typeParameters = typeParameters,
    valueParameters = valueParameters,
    returnType = returnType,
    receiverType = receiverType,
    annotations = annotations,
    analysisSession = analysisSession,
    token = token,
), CaVariableSignature<S>

/**
 * 从公开 callable symbol 构造 use-site 签名。
 *
 * 这里统一复用 session 级 callable-signature cache，保证同一 symbol
 * 在同一 use-site session 中只暴露一份稳定签名对象。
 */
internal fun <S : CaCallableSymbol> CaCfirSession.renderSignature(symbol: S): CaSignature<S> {
    val cfirSymbol = symbol as? CaCfirCallableSymbolSupport<*>
        ?: error("CFIR 签名构造仅支持 CFIR public symbol：${symbol::class.simpleName}")
    val cacheKey = cfirSymbol.publicSymbolCacheKeyOrNull() as? CaCfirCallableSymbolCacheKey
        ?: error("缺少稳定 callable-signature cache key：${symbol::class.simpleName}")
    @Suppress("UNCHECKED_CAST")
    return getOrCreateCallableSignature(cacheKey) {
        buildSignature(symbol)
    } as CaSignature<S>
}

/**
 * 从 PSI callable 声明恢复公开签名。
 *
 * PSI 侧不再单独发明签名构造路径，而是先恢复公开 symbol，
 * 再复用统一的 `symbol -> asSignature()` 语义主线。
 */
internal fun CaCfirSession.renderSignature(declaration: CjCallableDeclaration): CaSignature<CaCallableSymbol>? {
    val symbol = getPublicCallableSymbol(declaration) ?: return null
    return renderSignature(symbol)
}

private fun CaCfirSession.getPublicCallableSymbol(declaration: CjCallableDeclaration): CaCallableSymbol? {
    return symbolQueries.lookupSymbolsByPsi(declaration)
        .map { symbol -> getPublicSymbol(symbol) }
        .filterIsInstance<CaCallableSymbol>()
        .singleOrNull()
}

private fun <S : CaCallableSymbol> CaCfirSession.buildSignature(symbol: S): CaSignature<S> {
    val typeParameters = when (symbol) {
        is CaTypeParameterOwnerSymbol -> symbol.typeParameters
        else -> emptyList()
    }
    val valueParameters = buildValueParameterSignatures(symbol)
    val annotations = (symbol as? CaDeclarationSymbol)?.annotations.orEmpty()

    @Suppress("UNCHECKED_CAST")
    return when (symbol) {
        is CaFunctionSymbol -> CaCfirFunctionSignatureImpl(
            symbol = symbol,
            typeParameters = typeParameters,
            valueParameters = valueParameters,
            returnType = symbol.returnType,
            receiverType = symbol.receiverType,
            annotations = annotations,
            analysisSession = this,
            token = token,
        ) as CaSignature<S>

        is CaVariableSymbol -> CaCfirVariableSignatureImpl(
            symbol = symbol,
            typeParameters = typeParameters,
            valueParameters = valueParameters,
            returnType = symbol.returnType,
            receiverType = symbol.receiverType,
            annotations = annotations,
            analysisSession = this,
            token = token,
        ) as CaSignature<S>

        else -> CaCfirSignatureImpl(
            symbol = symbol,
            typeParameters = typeParameters,
            valueParameters = valueParameters,
            returnType = symbol.returnType,
            receiverType = symbol.receiverType,
            annotations = annotations,
            analysisSession = this,
            token = token,
        )
    }
}

private fun CaCfirSession.buildValueParameterSignatures(symbol: CaCallableSymbol): List<CaValueParameterSignature> {
    return when (symbol) {
        is CaValueParameterOwnerSymbol -> symbol.valueParameters.map { parameter ->
            parameter.asPublicParameterSignature(token)
        }

        is CaEnumConstructorSymbol -> symbol.payloadTypes.map { payloadType ->
            CaCfirValueParameterSignatureImpl(
                name = null,
                type = payloadType,
                annotations = emptyList(),
                token = token,
            )
        }

        else -> emptyList()
    }
}

private fun CaValueParameterSymbol.asPublicParameterSignature(token: CaLifetimeToken): CaValueParameterSignature {
    return CaCfirValueParameterSignatureImpl(
        name = name,
        type = returnType,
        annotations = annotations,
        token = token,
    )
}
