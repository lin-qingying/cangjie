package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolBase
import org.cangnova.cangjie.analysis.api.cfir.types.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.signatures.CaValueParameterSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjParameter

/**
 * value parameter 的公开签名快照。
 *
 * 这层只承载签名语义，不混入注解值转换或默认导入逻辑。
 */
internal class CaCfirValueParameterSignatureImpl(
    override val name: Name?,
    override val type: org.cangnova.cangjie.analysis.api.types.CaType?,
    override val annotations: List<CaAnnotation>,
    override val token: CaLifetimeToken,
) : CaValueParameterSignature

/**
 * callable 的公开签名快照。
 */
internal open class CaCfirSignatureImpl(
    override val declarationName: Name?,
    override val typeParameters: List<Name>,
    override val valueParameters: List<CaValueParameterSignature>,
    override val returnType: org.cangnova.cangjie.analysis.api.types.CaType?,
    override val annotations: List<CaAnnotation>,
    override val token: CaLifetimeToken,
) : CaSignature

/**
 * 从源码 callable 声明构建结构化签名。
 */
internal fun CaCfirSession.renderSignature(declaration: CjCallableDeclaration): CaSignature {
    return getOrCreateDeclarationSignature(declaration) {
        CaCfirSignatureImpl(
            declarationName = declaration.nameAsName,
            typeParameters = declaration.typeParameters.mapNotNull { typeParameter -> typeParameter.nameAsName },
            valueParameters = declaration.valueParameters.map { parameter ->
                parameter.asPublicParameterSignature(this, token)
            },
            returnType = with(this) { declaration.returnType },
            annotations = renderAnnotations(declaration),
            token = token,
        )
    }
}

/**
 * 从公开 callable 符号恢复结构化签名。
 *
 * 这里复用 session 级 callable-signature cache，保持与 Kotlin FIR 侧按稳定 key 缓存的做法一致。
 */
internal fun CaCfirSession.renderSignature(symbol: CaCallableSymbol): CaSignature? {
    val cfirSymbol = symbol as? CaCfirCallableSymbolBase<*> ?: return null
    val cacheKey = cfirSymbol.publicSymbolCacheKeyOrNull() as? CaCfirCallableSymbolCacheKey ?: return null
    return getOrCreateCallableSignature(cacheKey) {
        val declaration = cfirSymbol.psi as? CjCallableDeclaration ?: return@getOrCreateCallableSignature null
        renderSignature(declaration)
    }
}

private fun CjParameter.asPublicParameterSignature(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaValueParameterSignature {
    return CaCfirValueParameterSignatureImpl(
        name = nameAsName,
        type = session.queryValueParameterType(this)?.asCaType(session),
        annotations = session.renderAnnotations(this),
        token = token,
    )
}
