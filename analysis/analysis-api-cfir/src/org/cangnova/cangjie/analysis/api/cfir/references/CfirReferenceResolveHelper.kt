package org.cangnova.cangjie.analysis.api.cfir.references

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.buildSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression

/**
 * simple-name reference 到 Analysis API symbol 的解析桥。
 *
 * 对齐 Kotlin `FirReferenceResolveHelper.resolveSimpleNameReference` 的职责：
 * PSI 侧只决定要解析哪个表达式，CFIR 侧根据 resolved/candidate/error reference
 * 统一转换为公开 symbol。仓颉没有 Kotlin 的 safe-call、synthetic Java property、
 * resolved qualifier 等语义，因此这些分支不出现。
 */
internal object CfirReferenceResolveHelper {
    fun resolveSimpleNameReference(
        ref: CaCfirSimpleNameReference,
        analysisSession: CaCfirSession,
    ): Collection<org.cangnova.cangjie.analysis.api.symbols.CaSymbol> {
        val expression = ref.expression
        val symbolBuilder = analysisSession.cfirSymbolBuilder
        val adjustedResolutionExpression = adjustResolutionExpression(expression)
        val cfir = adjustedResolutionExpression.getOrBuildCfir(analysisSession.resolutionFacade)

        return when (cfir) {
            is CfirResolvable -> getSymbolsByResolvable(cfir, symbolBuilder)
            is CfirResolvedNamedReference -> cfir.toTargetSymbol(symbolBuilder)
            is CfirErrorNamedReference -> cfir.toTargetSymbol(symbolBuilder)
            else -> emptyList()
        }
    }

    private fun adjustResolutionExpression(expression: CjSimpleNameExpression): org.cangnova.cangjie.psi.CjElement {
        val parentAsCall = expression.parent as? CjCallExpression
        return parentAsCall ?: expression
    }

    private fun getSymbolsByResolvable(
        cfir: CfirResolvable,
        symbolBuilder: CaSymbolByCfirBuilder,
    ): Collection<org.cangnova.cangjie.analysis.api.symbols.CaSymbol> {
        return cfir.calleeReference.toTargetSymbol(symbolBuilder)
    }

    private fun CfirReference.toTargetSymbol(
        symbolBuilder: CaSymbolByCfirBuilder,
    ): Collection<org.cangnova.cangjie.analysis.api.symbols.CaSymbol> {
        return when (this) {
            is CfirResolvedNamedReference -> listOf(symbolBuilder.buildSymbol(resolvedSymbol))
            is org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate ->
                listOf(symbolBuilder.buildSymbol(candidateSymbol))
            is CfirErrorNamedReference -> {
                val diagnostic = diagnostic as? ConeDiagnosticWithCandidates ?: return emptyList()
                diagnostic.candidateSymbols.map(symbolBuilder::buildSymbol)
            }
            is CfirThisReference -> listOfNotNull(boundSymbol?.buildSymbol(symbolBuilder))
            is CfirSuperReference -> emptyList()
            else -> emptyList()
        }
    }
}
