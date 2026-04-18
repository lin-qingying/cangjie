package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendMemberCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPsiSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.getClassLikePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreCallablePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreExtendMemberCallablePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreExtendPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolBase
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbolBase
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbolImpl
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbolImpl
import org.cangnova.cangjie.analysis.api.components.CaVisibilityChecker
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * 当前 session 视角下的公开 symbol 可见性判定。
 *
 * 这里的“可见”语义是：该 symbol 是否仍然能够通过当前会话协议稳定恢复为同一公开对象。
 */
internal class CaCfirVisibilityChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaVisibilityChecker, CaCfirSessionComponent {
    override fun CaSymbol.isVisible(): Boolean = withValidityAssertion {
        when (this@isVisible) {
            is CaPackageSymbol -> analysisSession.scopeQueries.hasVisiblePackage(fqName)
            is CaCfirFileSymbolImpl -> analysisSession.symbolQueries.lookupFileSymbol(file) != null
            is CaExtendSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirExtendSymbolImpl -> analysisSession.restoreExtendPublicSymbol(stableIdentity)
                    else -> null
                }
                restoredSymbol === this@isVisible
            }

            is CaClassLikeSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirClassLikeSymbolBase<*> -> classId?.let(analysisSession::getClassLikePublicSymbol)
                    else -> null
                }
                restoredSymbol === this@isVisible
            }

            is CaCallableSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirCallableSymbolBase<*> -> when (val cacheKey = publicSymbolCacheKeyOrNull()) {
                        is CaCfirCallableSymbolCacheKey -> analysisSession.restoreCallablePublicSymbol(cacheKey.callableId, cacheKey.kind)
                        is CaCfirExtendMemberCallableSymbolCacheKey -> {
                            analysisSession.restoreExtendMemberCallablePublicSymbol(
                                extendIdentity = cacheKey.extendIdentity,
                                callableName = cacheKey.callableName,
                                kind = cacheKey.kind,
                            )
                        }
                        is CaCfirPsiSymbolCacheKey -> psi?.let { psiElement ->
                            analysisSession.symbolQueries.lookupSymbolsByPsi(psiElement)
                                .map(analysisSession::getPublicSymbol)
                                .singleOrNull { candidate -> candidate === this@isVisible }
                        }
                        else -> null
                    }
                    else -> null
                }
                restoredSymbol === this@isVisible
            }

            else -> false
        }
    }
}
