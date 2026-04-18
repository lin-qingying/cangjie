/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.structure

import org.cangnova.cangjie.analysis.api.platform.statistics.KotlinObjectSizeCalculator
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirLibrarySession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsOnlyApi
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLKotlinStubBasedLibrarySymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import kotlin.time.DurationUnit

@OptIn(LLStatisticsOnlyApi::class)
internal object LLSessionStatisticsCalculator {
    /**
     * Calculates the weight and other statistics for the given [session].
     */
    fun calculateSessionStatistics(session: LLCfirSession): LLSessionStatistics {
        val objectSizeCalculator = KotlinObjectSizeCalculator.getInstance(session.project)

        return context(objectSizeCalculator) {
            when (session) {
                is LLCfirResolvableModuleSession -> calculateResolvableSessionStatistics(session)
                is LLCfirLibrarySession -> calculateLibrarySessionStatistics(session)
                else -> LLSessionStatistics.ZERO
            }
        }
    }

    context(_: KotlinObjectSizeCalculator?)
    private fun calculateResolvableSessionStatistics(session: LLCfirResolvableModuleSession): LLSessionStatistics {
        val moduleFileCache = session.moduleComponents.cache
        val firFiles = moduleFileCache.getAllCachedCfirFiles()

        val kotlinWeight = calculateCfirElementWeight(firFiles)
        return LLSessionStatistics(kotlinWeight, 0L, session.currentLifetime)
    }

    context(_: KotlinObjectSizeCalculator?)
    private fun calculateLibrarySessionStatistics(session: LLCfirLibrarySession): LLSessionStatistics {
        val symbolProviders = (session.symbolProvider as? LLModuleWithDependenciesSymbolProvider)?.providers
            ?: return LLSessionStatistics.ZERO

        val kotlinWeight = symbolProviders
            .filterIsInstance<LLKotlinStubBasedLibrarySymbolProvider>()
            .sumOf { calculateCfirElementWeight(it.cachedDeclarations) }

        return LLSessionStatistics(kotlinWeight, 0L, session.currentLifetime)
    }

    private val LLCfirSession.currentLifetime: Double
        get() = creationTimeMark.elapsedNow().toDouble(DurationUnit.SECONDS)

    context(objectSizeCalculator: KotlinObjectSizeCalculator?)
    private fun calculateCfirElementWeight(firElements: Collection<CfirElement>): Long {
        if (objectSizeCalculator == null) return 0L

        return firElements.sumOf { calculateCfirElementWeight(it) }
    }

    context(objectSizeCalculator: KotlinObjectSizeCalculator)
    private fun calculateCfirElementWeight(firElement: CfirElement): Long {
        val visitor = CfirElementWeightCalculatorVisitor(objectSizeCalculator)
        firElement.accept(visitor, null)
        return visitor.totalWeight
    }

    /**
     * CFIR element weight calculation is an approximation. See [LLSessionStatistics] for details.
     */
    private class CfirElementWeightCalculatorVisitor(private val objectSizeCalculator: KotlinObjectSizeCalculator) : CfirVisitorVoid() {
        var totalWeight = 0L
            private set

        override fun visitElement(element: CfirElement) {
            totalWeight += objectSizeCalculator.shallowSize(element)
            element.acceptChildren(this, null)
        }
    }
}
