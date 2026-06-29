package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreExtendPublicSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * package/file/class-like/extend 这类稳定声明身份的专用 pointer。
 *
 * 它们直接绑定各自的稳定公开 cache key，而不是再包一层统一 restore-key。
 */
internal class CaCfirPackageSymbolPointer(
    /**
     * 包符号的稳定缓存键。
     */
    private val cacheKey: CaCfirPackageSymbolCacheKey,
) : CaCfirSymbolPointerBase<CaPackageSymbol>() {
    /**
     * 在目标 CFIR session 中按包名恢复包符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPackageSymbol? =
        restoreSession(session)?.getPackageSymbol(cacheKey.fqName)
}

/**
 * 文件符号 pointer。
 */
internal class CaCfirFileSymbolPointer(
    /**
     * 文件符号的稳定缓存键。
     */
    private val cacheKey: CaCfirFileSymbolCacheKey,
) : CaCfirSymbolPointerBase<org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol>() {
    /**
     * 在目标 CFIR session 中从文件 PSI 恢复文件符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol? =
        restoreSession(session)?.let { with(it) { cacheKey.file.symbol } }
}

/**
 * class-like 符号 pointer。
 */
internal class CaCfirClassLikeSymbolPointer<S : CaSymbol>(
    /**
     * class-like 符号的稳定缓存键。
     */
    private val cacheKey: CaCfirClassLikeSymbolCacheKey,
    /**
     * 恢复后需要满足的公开符号类型。
     */
    private val symbolType: Class<S>,
) : CaCfirSymbolPointerBase<S>() {
    /**
     * 在目标 CFIR session 中按 classId 恢复 class-like 符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): S? =
        symbolType.castOrNull(restoreSession(session)?.getClassLikeSymbol(cacheKey.classId))
}

/**
 * extend 符号 pointer。
 */
internal class CaCfirExtendSymbolPointer(
    /**
     * extend 符号的稳定缓存键。
     */
    private val cacheKey: CaCfirExtendSymbolCacheKey,
) : CaCfirSymbolPointerBase<org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol>() {
    /**
     * 在目标 CFIR session 中按稳定 extend identity 恢复 extend 符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol? =
        restoreSession(session)?.restoreExtendPublicSymbol(cacheKey.identity)
}
