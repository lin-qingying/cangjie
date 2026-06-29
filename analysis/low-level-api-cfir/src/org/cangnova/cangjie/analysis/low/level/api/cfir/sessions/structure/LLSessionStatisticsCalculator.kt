

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

/**
 * 根据 session 当前缓存内容计算 session structure 图使用的统计信息。
 */
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

    /**
     * 计算可解析源码类 session 的 CFIR 元素权重。
     */
    private fun calculateResolvableSessionStatistics(session: LLCfirResolvableModuleSession): LLSessionStatistics {
        val moduleFileCache = session.moduleComponents.cache
        val cfirFiles = moduleFileCache.getAllCachedCfirFiles()

        val cangjieWeight = calculateCfirElementWeight(cfirFiles)
        return LLSessionStatistics(cangjieWeight, 0L, session.currentLifetime)
    }

    /**
     * 计算二进制库 session 中 stub-based CangJie 库声明的 CFIR 元素权重。
     */
    private fun calculateLibrarySessionStatistics(session: LLCfirLibrarySession): LLSessionStatistics {
        val symbolProviders = (session.symbolProvider as? LLModuleWithDependenciesSymbolProvider)?.providers
            ?: return LLSessionStatistics.ZERO

        val cangjieWeight = symbolProviders
            .filterIsInstance<LLCangJieStubBasedLibrarySymbolProvider>()
            .sumOf { calculateCfirElementWeight(it.cachedDeclarations) }

        return LLSessionStatistics(cangjieWeight, 0L, session.currentLifetime)
    }

    /**
     * 当前 session 的存活时间，单位为秒。
     */
    private val LLCfirSession.currentLifetime: Double
        get() = creationTimeMark.elapsedNow().toDouble(DurationUnit.SECONDS)

    /**
     * 计算一组 CFIR 元素的累计权重。
     */
    private fun calculateCfirElementWeight(cfirElements: Collection<CfirElement>): Long {
        return cfirElements.sumOf { calculateCfirElementWeight(it) }
    }

    /**
     * 计算单个 CFIR 元素子树的权重。
     */
    private fun calculateCfirElementWeight(cfirElement: CfirElement): Long {
        val visitor = CfirElementWeightCalculatorVisitor()
        cfirElement.accept(visitor, null)
        return visitor.totalWeight
    }

    /**
     * CFIR element weight calculation is an approximation. See [LLSessionStatistics] for details.
     */
    private class CfirElementWeightCalculatorVisitor : CfirVisitorVoid() {
        /**
         * 当前已访问 CFIR 元素数量。
         */
        var totalWeight = 0L
            private set

        /**
         * 将当前 [element] 计入权重并继续访问子元素。
         */
        override fun visitElement(element: CfirElement) {
            totalWeight += 1
            element.acceptChildren(this, null)
        }
    }
}
