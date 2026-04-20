package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.createFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.getClassLikePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPackagePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreExtendPublicSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * package/file/class-like/extend 这类稳定声明身份的专用 pointer。
 *
 * 它们直接绑定各自的稳定公开 cache key，而不是再包一层统一 restore-key。
 */
internal class CaCfirPackageSymbolPointer(
    private val cacheKey: CaCfirPackageSymbolCacheKey,
) : CaCfirSymbolPointerBase<CaPackageSymbol>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPackageSymbol? =
        restoreSession(session)?.getPackagePublicSymbol(cacheKey.fqName)
}

internal class CaCfirFileSymbolPointer(
    private val cacheKey: CaCfirFileSymbolCacheKey,
) : CaCfirSymbolPointerBase<org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol? =
        restoreSession(session)?.createFileSymbol(cacheKey.file)
}

internal class CaCfirClassLikeSymbolPointer<S : CaSymbol>(
    private val cacheKey: CaCfirClassLikeSymbolCacheKey,
    private val symbolType: Class<S>,
) : CaCfirSymbolPointerBase<S>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): S? =
        symbolType.castOrNull(restoreSession(session)?.getClassLikePublicSymbol(cacheKey.classId))
}

internal class CaCfirExtendSymbolPointer(
    private val cacheKey: CaCfirExtendSymbolCacheKey,
) : CaCfirSymbolPointerBase<org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol? =
        restoreSession(session)?.restoreExtendPublicSymbol(cacheKey.identity)
}
