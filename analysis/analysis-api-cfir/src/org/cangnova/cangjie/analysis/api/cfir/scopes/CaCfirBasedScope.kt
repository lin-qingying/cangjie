package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.completionDecisionKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin `KaFirBasedScope` 的公开作用域底座。
 *
 * 这里直接包装真实的 `CfirContainingNamesAwareScope`，Analysis API 侧只负责：
 * 1. 把底层 CFIR symbol 映射为公开 `CaSymbol`
 * 2. 在 file/package 等场景补充宿主 eager symbol
 * 3. 统一做按名称查询与去重
 */
internal abstract class CaCfirBasedScope<S : CfirContainingNamesAwareScope>(
    protected val primaryScope: S,
    protected val analysisSession: CaCfirSession,
    final override val token: CaLifetimeToken,
) : CaScope {
    protected open val additionalScopes: List<CfirContainingNamesAwareScope>
        get() = emptyList()

    protected open val eagerSymbols: List<CaSymbol>
        get() = emptyList()

    override val availableNames: Set<Name>
        get() = withValidityAssertion {
            buildSet {
                scopes().forEach { scope ->
                    addAll(scope.getCallableNames())
                    addAll(scope.getClassifierNames())
                }
                eagerSymbols.mapNotNullTo(this) { symbol -> symbol.name }
            }
        }

    override val symbols: List<CaSymbol>
        get() = withValidityAssertion {
            buildList {
                addAll(eagerSymbols)
                availableNames.forEach { name ->
                    addAll(getSymbols(name))
                }
            }.distinctBy { symbol -> symbol.completionDecisionKey() }
        }

    override fun getSymbols(name: Name): List<CaSymbol> = withValidityAssertion {
        (eagerSymbols.filter { symbol -> symbol.name == name } + collectScopeSymbols(name).map(analysisSession::getPublicSymbol))
            .distinctBy { symbol -> symbol.completionDecisionKey() }
    }

    override fun getCallableSymbols(name: Name): List<CaCallableSymbol> = withValidityAssertion {
        collectCallableSymbols(name)
            .map(analysisSession::getPublicSymbol)
            .filterIsInstance<CaCallableSymbol>()
            .distinctBy { symbol -> symbol.completionDecisionKey() }
    }

    override fun getClassifierSymbols(name: Name): List<CaClassifierSymbol> = withValidityAssertion {
        collectClassifierSymbols(name)
            .map(analysisSession::getPublicSymbol)
            .filterIsInstance<CaClassifierSymbol>()
            .distinctBy { symbol -> symbol.completionDecisionKey() }
    }

    private fun scopes(): List<CfirContainingNamesAwareScope> = listOf(primaryScope) + additionalScopes

    private fun collectScopeSymbols(name: Name): List<CfirBasedSymbol<*>> {
        return buildList {
            scopes().forEach { scope ->
                scope.processClassifiersByName(name) { symbol -> add(symbol) }
                scope.processCallablesByName(name) { symbol -> add(symbol) }
            }
        }.distinctBy { symbol -> symbol.scopeIdentity() }
    }

    private fun collectCallableSymbols(name: Name): List<CfirCallableSymbol<*>> {
        return buildList {
            scopes().forEach { scope ->
                scope.processCallablesByName(name) { symbol -> add(symbol) }
            }
        }.distinctBy { symbol -> symbol.scopeIdentity() }
    }

    private fun collectClassifierSymbols(name: Name): List<CfirClassLikeSymbol<*>> {
        return buildList {
            scopes().forEach { scope ->
                scope.processClassifiersByName(name) { symbol -> add(symbol) }
            }
        }.distinctBy { symbol -> symbol.scopeIdentity() }
    }
}

private fun CfirBasedSymbol<*>.scopeIdentity(): String = when (this) {
    is CfirClassLikeSymbol<*> -> "class:${classId.asString()}"
    is CfirCallableSymbol<*> -> "callable:${callableId?.toString() ?: name.asString()}"
    is CfirFileSymbol -> "file:${cfir.name}"
    else -> "${this::class.qualifiedName}:$debugName"
}
