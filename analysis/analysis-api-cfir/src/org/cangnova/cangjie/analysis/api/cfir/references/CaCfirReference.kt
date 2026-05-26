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

@OptIn(CaImplementationDetail::class)
internal sealed interface CaCfirReference : CjReference {
    override val resolver get() = CaCfirReferenceResolver

    fun isReferenceToImportAlias(alias: CjImportAlias): Boolean {
        return getImportAlias(alias.importDirective) != null
    }

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

    fun getResolvedToPsi(analysisSession: CaSession, referenceTargetSymbols: Collection<CaSymbol>): Collection<PsiElement> =
        with(analysisSession) {
            referenceTargetSymbols.flatMap { symbol ->
                when (symbol) {
                    is CaCfirSymbol<*> -> getPsiDeclarations(symbol)
                    else -> listOfNotNull(symbol.psi)
                }
            }
        }

    fun getResolvedToPsi(analysisSession: CaSession): Collection<PsiElement> =
        with(analysisSession) {
            getResolvedToPsi(analysisSession, resolveToSymbols())
        }

    @OptIn(CaPlatformInterface::class)
    fun CaSession.resolveToSymbols(): Collection<CaSymbol> = withValidityAssertion {
        check(this is CaCfirSession)
        this.cacheStorage.resolveToSymbolsCache.value.getOrPut(this@CaCfirReference) {
            computeSymbols()
        }
    }
}

internal fun CaSession.getPsiDeclarations(symbol: CaCfirSymbol<*>): Collection<PsiElement> {
    val intersectionOverriddenSymbolsOrSingle = when {
        symbol.origin == CaSymbolOrigin.INTERSECTION_OVERRIDE && symbol is CaCallableSymbol -> symbol.intersectionOverriddenSymbols
        else -> listOf(symbol)
    }
    return intersectionOverriddenSymbolsOrSingle.mapNotNull { it.findPsiForReferenceResolve(analysisScope) }
}


private fun CaSymbol.findPsiForReferenceResolve(scope: GlobalSearchScope): PsiElement? {
    require(this is CaCfirSymbol<*>)
    return cfirSymbol.cfir.findReferencePsi(scope, analysisSession.project)
}
