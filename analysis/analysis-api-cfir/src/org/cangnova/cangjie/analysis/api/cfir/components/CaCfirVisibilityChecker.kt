package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendMemberCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPsiSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreCallablePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreExtendMemberCallablePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreExtendPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.components.CaVisibilityChecker
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 当前 session 视角下的公开 symbol 可见性判定。
 *
 * 这里的“可见”语义是：该 symbol 是否仍然能够通过当前会话协议稳定恢复为同一公开对象。
 */
@OptIn(CaPlatformInterface::class)
internal class CaCfirVisibilityChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaVisibilityChecker, CaCfirSessionComponent {
    override fun CaSymbol.isVisible(): Boolean = withValidityAssertion {
        when (this@isVisible) {
            is CaPackageSymbol -> analysisSession.useSitePackageProvider.doesPackageExist(fqName)
            is CaCfirFileSymbol -> runCatching { file.getOrBuildCfirFile(analysisSession.resolutionFacade) }.isSuccess
            is CaExtendSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirExtendSymbol -> analysisSession.restoreExtendPublicSymbol(stableIdentity)
                    else -> null
                }
                restoredSymbol === this@isVisible
            }

            is CaClassLikeSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    else -> classId?.let(analysisSession::getClassLikeSymbol)
                }
                restoredSymbol === this@isVisible
            }

            is CaCallableSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirSymbol<*> -> when (val cacheKey = publicSymbolCacheKeyOrNull()) {
                        is CaCfirCallableSymbolCacheKey -> analysisSession.restoreCallablePublicSymbol(cacheKey.callableId, cacheKey.kind)
                        is CaCfirExtendMemberCallableSymbolCacheKey -> {
                            analysisSession.restoreExtendMemberCallablePublicSymbol(
                                extendIdentity = cacheKey.extendIdentity,
                                callableName = cacheKey.callableName,
                                kind = cacheKey.kind,
                            )
                        }
                        is CaCfirPsiSymbolCacheKey -> psi?.let { psiElement ->
                            analysisSession.symbolByPsi(psiElement)
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

    private fun CaCfirSession.symbolByPsi(psi: com.intellij.psi.PsiElement): CaSymbol? = with(this) {
        when (psi) {
            is CjParameter -> psi.symbol
            is CjTypeStatement -> psi.classSymbol
            is CjTypeAlias -> psi.symbol
            is CjNamedFunction -> psi.symbol
            is CjFunctionLiteral -> psi.symbol
            is CjConstructor<*> -> psi.symbol
            is CjMacroDeclaration -> psi.symbol
            is CjFinalizer -> psi.symbol
            is CjProperty -> psi.symbol
            is CjPropertyAccessor -> psi.symbol
            is CjFieldVariable -> psi.symbol
            is CjEnumConstructor -> psi.symbol
            is CjPatternVariable -> psi.symbol
            is CjBindingPattern -> psi.symbol
            is CjExtend -> psi.symbol
            is CjTypeParameter -> psi.symbol
            else -> null
        }
    }
}
