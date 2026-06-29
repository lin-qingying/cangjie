package org.cangnova.cangjie.analysis.api.cfir.references

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findReferencePsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.platform.caches.getOrPut
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.idea.references.CjReference
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjImportInfo
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjSimpleNameExpression

/**
 * CFIR Analysis API 支持的仓颉 PSI 引用公共接口。
 */
@OptIn(CaImplementationDetail::class)
internal sealed interface CaCfirReference : CjReference {
    /**
     * 当前引用使用的 CFIR 多结果解析器。
     */
    override val resolver get() = CaCfirReferenceResolver

    /**
     * 判断当前引用是否指向给定 import alias。
     */
    fun isReferenceToImportAlias(alias: CjImportAlias): Boolean {
        return getImportAlias(alias.importDirective) != null
    }

    /**
     * 在 import 信息中查找与当前引用目标等价的 alias。
     */
    fun getImportAlias(importInfo: CjImportInfo?): CjImportAlias? {
        val importItem = importInfo as? CjImportItem ?: return null
        val importedReference = importItem.importedReference ?: return null
        val importResults =
            when (importedReference) {
                is CjDotQualifiedExpression -> importedReference.selectorExpression?.mainReference?.multiResolve(false)
                is CjSimpleNameExpression -> importedReference.mainReference.multiResolve(false)
                else -> null
            } ?: return null
        val targets = multiResolve(false).mapNotNull { it.element }
        val adjustedImportTargets = importResults.mapNotNull { it.element }
        val manager = importItem.manager
        if (adjustedImportTargets.any { importTarget ->
                targets.any { target ->
                    manager.areElementsEquivalent(target, importTarget)
                }
            }) {
            return importItem.alias
        }
        return null
    }

    /**
     * The result of this method will be used by [resolveToSymbols] and can be cached
     */
    fun CaCfirSession.computeSymbols(): Collection<CaSymbol>

    /**
     * 将解析得到的公开符号转换为 PSI 目标集合。
     */
    fun getResolvedToPsi(analysisSession: CaSession, referenceTargetSymbols: Collection<CaSymbol>): Collection<PsiElement> =
        with(analysisSession) {
            referenceTargetSymbols.flatMap { symbol ->
                when (symbol) {
                    is CaCfirSymbol<*> -> getPsiDeclarations(symbol)
                    else -> listOfNotNull(symbol.psi)
                }
            }
        }

    /**
     * 解析当前引用并返回 PSI 目标集合。
     */
    fun getResolvedToPsi(analysisSession: CaSession): Collection<PsiElement> =
        with(analysisSession) {
            getResolvedToPsi(analysisSession, resolveToSymbols())
        }

    /**
     * 在 CFIR session 缓存中解析当前引用对应的公开符号集合。
     */
    @OptIn(CaPlatformInterface::class)
    fun CaSession.resolveToSymbols(): Collection<CaSymbol> = withValidityAssertion {
        check(this is CaCfirSession)
        this.cacheStorage.resolveToSymbolsCache.value.getOrPut(this@CaCfirReference) {
            computeSymbols()
        }
    }
}

/**
 * 将 CFIR 公开符号恢复为 reference resolve 可用的 PSI 声明集合。
 */
internal fun CaSession.getPsiDeclarations(symbol: CaCfirSymbol<*>): Collection<PsiElement> {
    val intersectionOverriddenSymbolsOrSingle = when {
        symbol.origin == CaSymbolOrigin.INTERSECTION_OVERRIDE && symbol is CaCallableSymbol -> symbol.intersectionOverriddenSymbols
        else -> listOf(symbol)
    }
    return intersectionOverriddenSymbolsOrSingle.mapNotNull { it.findPsiForReferenceResolve(analysisScope) }
}


/**
 * 恢复单个公开符号在给定搜索范围内的 reference resolve PSI。
 */
private fun CaSymbol.findPsiForReferenceResolve(scope: GlobalSearchScope): PsiElement? {
    require(this is CaCfirSymbol<*>)
    return cfirSymbol.cfir.findReferencePsi(scope, analysisSession.project)
}
