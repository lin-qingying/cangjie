

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.structure

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirLibrarySession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsOnlyApi
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieStubBasedLibrarySymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import kotlin.time.DurationUnit

@OptIn(LLStatisticsOnlyApi::class)
internal object LLSessionStatisticsCalculator {
    /**
     * Calculates the weight and other statistics for the given [session].
     */
    fun calculateSessionStatistics(session: LLCfirSession): LLSessionStatistics {
        return when (session) {
            is LLCfirResolvableModuleSession -> calculateResolvableSessionStatistics(session)
            is LLCfirLibrarySession -> calculateLibrarySessionStatistics(session)
            else -> LLSessionStatistics.ZERO
        }
    }

    private fun calculateResolvableSessionStatistics(session: LLCfirResolvableModuleSession): LLSessionStatistics {
        val moduleFileCache = session.moduleComponents.cache
        val cfirFiles = moduleFileCache.getAllCachedCfirFiles()

        val cangjieWeight = calculateCfirElementWeight(cfirFiles)
        return LLSessionStatistics(cangjieWeight, 0L, session.currentLifetime)
    }

    private fun calculateLibrarySessionStatistics(session: LLCfirLibrarySession): LLSessionStatistics {
        val symbolProviders = (session.symbolProvider as? LLModuleWithDependenciesSymbolProvider)?.providers
            ?: return LLSessionStatistics.ZERO

        val cangjieWeight = symbolProviders
            .filterIsInstance<LLCangJieStubBasedLibrarySymbolProvider>()
            .sumOf { calculateCfirElementWeight(it.cachedDeclarations) }

        return LLSessionStatistics(cangjieWeight, 0L, session.currentLifetime)
    }

    private val LLCfirSession.currentLifetime: Double
        get() = creationTimeMark.elapsedNow().toDouble(DurationUnit.SECONDS)

    private fun calculateCfirElementWeight(cfirElements: Collection<CfirElement>): Long {
        return cfirElements.sumOf { calculateCfirElementWeight(it) }
    }

    private fun calculateCfirElementWeight(cfirElement: CfirElement): Long {
        val visitor = CfirElementWeightCalculatorVisitor()
        cfirElement.accept(visitor, null)
        return visitor.totalWeight
    }

    /**
     * CFIR element weight calculation is an approximation. See [LLSessionStatistics] for details.
     */
    private class CfirElementWeightCalculatorVisitor : CfirVisitorVoid() {
        var totalWeight = 0L
            private set

        override fun visitElement(element: CfirElement) {
            totalWeight += 1
            element.acceptChildren(this, null)
        }
    }
}
