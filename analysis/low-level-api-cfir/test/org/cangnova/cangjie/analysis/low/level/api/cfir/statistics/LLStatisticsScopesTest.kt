package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CaDiagnosticCheckerSet
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectionPhase
import org.cangnova.cangjie.cfir.builder.CfirRawBuildSource
import org.cangnova.cangjie.cfir.builder.CfirRawBuildStage
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroConstructionStage
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome
import org.cangnova.cangjie.parsing.CangjiePsiParseKind
import org.junit.jupiter.api.Test

/**
 * [LLStatisticsScopes] 的指标命名空间测试。
 *
 * 指标最终由平台导出并按名字聚合，根 scope 必须落在仓颉自己的命名空间下，
 * 否则会与同进程内的 Kotlin Analysis API 指标混在一起。
 */
class LLStatisticsScopesTest {
    /**
     * 根 scope 必须是 `cangjie.analysis`。
     */
    @Test
    fun rootScopeUsesCangjieNamespace() {
        assertEquals("cangjie.analysis", LLStatisticsScopes.name)
    }

    /**
     * 所有派生 scope 都必须以根 scope 为前缀。
     */
    @Test
    fun derivedScopesArePrefixedWithRoot() {
        val derivedScopes = listOf(
            LLStatisticsScopes.AnalysisSessions,
            LLStatisticsScopes.AnalysisSessions.Analyze,
            LLStatisticsScopes.AnalysisSessions.Analyze.Invocations,
            LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup,
            LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup.Invocations,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Hits,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Misses,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Evictions,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache,
            LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache,
            LLStatisticsScopes.Resolve,
            LLStatisticsScopes.Resolve.Phases,
            LLStatisticsScopes.RawBuild,
            LLStatisticsScopes.Macro,
            LLStatisticsScopes.Macro.SymbolIndex,
            LLStatisticsScopes.Macro.ImportBinding,
            LLStatisticsScopes.Macro.Expansion,
            LLStatisticsScopes.Diagnostics,
            LLStatisticsScopes.Diagnostics.Collection,
            LLStatisticsScopes.Diagnostics.ElementCollection,
            LLStatisticsScopes.Diagnostics.StructureBuild,
            LLStatisticsScopes.Diagnostics.StructureElement,
            LLStatisticsScopes.Diagnostics.Pass,
            LLStatisticsScopes.SessionCreation,
            LLStatisticsScopes.Scopes,
            LLStatisticsScopes.Parse,
            LLStatisticsScopes.Deserialization,
            LLStatisticsScopes.Deserialization.ClassLike,
            LLStatisticsScopes.SymbolProviders,
            LLStatisticsScopes.SymbolProviders.Combined,
            LLStatisticsScopes.SymbolProviders.Combined.Classes,
            LLStatisticsScopes.SymbolProviders.Combined.Callables,
        )

        derivedScopes.forEach { scope ->
            assertTrue(
                scope.name.startsWith("${LLStatisticsScopes.name}."),
                "scope ${scope.name} 必须以根 scope ${LLStatisticsScopes.name} 为前缀。",
            )
        }
    }

    /**
     * 公开名称视图必须与 internal scope 同源：任何一边改名，另一边都要跟着改。
     *
     * 跨模块的性能测试模块只能看到 [LLStatisticsMetricNames]，看不到 internal 的
     * [LLStatisticsScopes]；两者一旦漂移，测试会对着不存在的指标做断言而不报错。
     */
    @Test
    @OptIn(LLStatisticsOnlyApi::class)
    fun publicMetricNamesMirrorInternalScopes() {
        assertEquals(LLStatisticsScopes.name, LLStatisticsMetricNames.root)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Analyze.Invocations.name, LLStatisticsMetricNames.analyzeInvocations)
        assertEquals(LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup.Invocations.name, LLStatisticsMetricNames.lowMemoryCacheCleanupInvocations)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.hits.name, LLStatisticsMetricNames.resolveCallCacheHits)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.misses.name, LLStatisticsMetricNames.resolveCallCacheMisses)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache.hits.name, LLStatisticsMetricNames.resolveSymbolCacheHits)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache.misses.name, LLStatisticsMetricNames.resolveSymbolCacheMisses)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache.hits.name, LLStatisticsMetricNames.resolveToSymbolsCacheHits)
        assertEquals(LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache.misses.name, LLStatisticsMetricNames.resolveToSymbolsCacheMisses)
        assertEquals(LLStatisticsScopes.SymbolProviders.Combined.Classes.hits.name, LLStatisticsMetricNames.combinedSymbolProviderClassCacheHits)
        assertEquals(LLStatisticsScopes.SymbolProviders.Combined.Classes.misses.name, LLStatisticsMetricNames.combinedSymbolProviderClassCacheMisses)
        assertEquals(LLStatisticsScopes.SymbolProviders.Combined.Callables.hits.name, LLStatisticsMetricNames.combinedSymbolProviderCallableCacheHits)
        assertEquals(LLStatisticsScopes.SymbolProviders.Combined.Callables.misses.name, LLStatisticsMetricNames.combinedSymbolProviderCallableCacheMisses)
        CfirResolvePhase.entries.forEach { phase ->
            assertEquals(LLStatisticsScopes.Resolve.Phases.duration(phase), LLStatisticsMetricNames.resolvePhaseDuration(phase))
            assertEquals(LLStatisticsScopes.Resolve.Phases.files(phase), LLStatisticsMetricNames.resolvePhaseFiles(phase))
            assertEquals(LLStatisticsScopes.Resolve.Phases.declarations(phase), LLStatisticsMetricNames.resolvePhaseDeclarations(phase))
            assertEquals(LLStatisticsScopes.Resolve.Phases.runs(phase), LLStatisticsMetricNames.resolvePhaseRuns(phase))
        }
        CfirRawBuildSource.entries.forEach { source ->
            CfirRawBuildStage.entries.forEach { stage ->
                assertEquals(
                    LLStatisticsScopes.RawBuild.duration(source, stage),
                    LLStatisticsMetricNames.rawBuildDuration(source, stage),
                )
                assertEquals(
                    LLStatisticsScopes.RawBuild.runs(source, stage),
                    LLStatisticsMetricNames.rawBuildRuns(source, stage),
                )
            }
        }
        CfirMacroConstructionStage.entries.forEach { stage ->
            assertEquals(LLStatisticsScopes.Macro.stage(stage).duration(), LLStatisticsMetricNames.macroConstructionDuration(stage))
            assertEquals(LLStatisticsScopes.Macro.stage(stage).runs(), LLStatisticsMetricNames.macroConstructionRuns(stage))
        }
        assertEquals(LLStatisticsScopes.Macro.Expansion.files(), LLStatisticsMetricNames.macroExpandFiles)
        assertEquals(LLStatisticsScopes.Macro.Expansion.surfaces(), LLStatisticsMetricNames.macroExpandSurfaces)
        CfirMacroExpansionOutcome.entries.forEach { outcome ->
            assertEquals(LLStatisticsScopes.Macro.Expansion.outcome(outcome), LLStatisticsMetricNames.macroExpandOutcome(outcome))
        }
        assertEquals(LLStatisticsScopes.Diagnostics.Collection.duration(), LLStatisticsMetricNames.diagnosticsCollectionDuration)
        assertEquals(LLStatisticsScopes.Diagnostics.Collection.runs(), LLStatisticsMetricNames.diagnosticsCollectionRuns)
        assertEquals(LLStatisticsScopes.Diagnostics.Collection.diagnostics(), LLStatisticsMetricNames.diagnosticsCollectionDiagnostics)
        assertEquals(LLStatisticsScopes.Diagnostics.ElementCollection.duration(), LLStatisticsMetricNames.diagnosticsElementCollectionDuration)
        assertEquals(LLStatisticsScopes.Diagnostics.ElementCollection.runs(), LLStatisticsMetricNames.diagnosticsElementCollectionRuns)
        assertEquals(LLStatisticsScopes.Diagnostics.StructureBuild.duration(), LLStatisticsMetricNames.diagnosticsStructureBuildDuration)
        assertEquals(LLStatisticsScopes.Diagnostics.StructureBuild.runs(), LLStatisticsMetricNames.diagnosticsStructureBuildRuns)
        CaDiagnosticCheckerSet.entries.forEach { set ->
            assertEquals(
                LLStatisticsScopes.Diagnostics.StructureElement.duration(set),
                LLStatisticsMetricNames.diagnosticsStructureElementDuration(set),
            )
            assertEquals(
                LLStatisticsScopes.Diagnostics.StructureElement.runs(set),
                LLStatisticsMetricNames.diagnosticsStructureElementRuns(set),
            )
            assertEquals(
                LLStatisticsScopes.Diagnostics.StructureElement.diagnostics(set),
                LLStatisticsMetricNames.diagnosticsStructureElementDiagnostics(set),
            )
        }
        DiagnosticCollectionPhase.entries.forEach { phase ->
            assertEquals(LLStatisticsScopes.Diagnostics.Pass.duration(phase), LLStatisticsMetricNames.diagnosticsPassDuration(phase))
            assertEquals(LLStatisticsScopes.Diagnostics.Pass.runs(phase), LLStatisticsMetricNames.diagnosticsPassRuns(phase))
        }
        CaModuleKind.entries.filter { it != CaModuleKind.UNKNOWN }.forEach { kind ->
            assertEquals(LLStatisticsScopes.SessionCreation.duration(kind), LLStatisticsMetricNames.sessionCreationDuration(kind))
            assertEquals(LLStatisticsScopes.SessionCreation.runs(kind), LLStatisticsMetricNames.sessionCreationRuns(kind))
        }
        assertEquals(LLStatisticsScopes.Scopes.sessionCreated(), LLStatisticsMetricNames.scopeSessionsCreated)
        assertEquals(LLStatisticsScopes.Deserialization.ClassLike.duration(), LLStatisticsMetricNames.deserializationClassLikeDuration)
        assertEquals(LLStatisticsScopes.Deserialization.ClassLike.runs(), LLStatisticsMetricNames.deserializationClassLikeRuns)
        CangjiePsiParseKind.entries.forEach { kind ->
            assertEquals(LLStatisticsScopes.Parse.duration(kind), LLStatisticsMetricNames.psiParseDuration(kind))
            assertEquals(LLStatisticsScopes.Parse.runs(kind), LLStatisticsMetricNames.psiParseRuns(kind))
        }
    }

    /**
     * 模块种类的 JFR 编号是对外契约：既有消费者按这些数字分类，改动会让历史事件错位。
     */
    @Test
    fun moduleKindJfrCodesStayStable() {
        val expected = mapOf(
            CaModuleKind.SOURCE to 0.toByte(),
            CaModuleKind.DANGLING_FILE to 1.toByte(),
            CaModuleKind.NOT_UNDER_CONTENT_ROOT to 2.toByte(),
            CaModuleKind.FALLBACK_DEPENDENCIES to 3.toByte(),
            CaModuleKind.LIBRARY to 4.toByte(),
            CaModuleKind.LIBRARY_SOURCE to 5.toByte(),
            CaModuleKind.BUILTINS to 6.toByte(),
            CaModuleKind.UNKNOWN to (-1).toByte(),
        )
        assertEquals(expected, CaModuleKind.entries.associateWith { it.jfrCode })
    }

    /**
     * 每个遍历阶段都必须有独立的 pass scope：漏登记会让该阶段既不记耗时也不记次数。
     */
    @Test
    fun everyDiagnosticPassPhaseHasItsOwnScope() {
        val names = DiagnosticCollectionPhase.entries.map { LLStatisticsScopes.Diagnostics.Pass.duration(it) }
        assertEquals(DiagnosticCollectionPhase.entries.size, names.toSet().size, "每个遍历阶段必须有独立的 pass scope")
    }

    /**
     * 每个 PSI 解析入口都必须有独立的 parse scope：漏登记会让该入口既不记耗时也不记次数。
     */
    @Test
    fun everyPsiParseKindHasItsOwnScope() {
        val names = CangjiePsiParseKind.entries.map { LLStatisticsScopes.Parse.duration(it) }
        assertEquals(CangjiePsiParseKind.entries.size, names.toSet().size, "每个 PSI 解析入口必须有独立的 parse scope")
    }

    /**
     * 指标段必须与模块种类一一对应：漏登记会让该类别的 session 创建既不记耗时也不记次数。
     */
    @Test
    fun sessionCreationScopesAreUniquePerModuleKind() {
        val mapped = CaModuleKind.entries.filter { it != CaModuleKind.UNKNOWN }.associateWith { LLStatisticsScopes.SessionCreation.duration(it) }
        assertEquals(mapped.size, mapped.values.toSet().size, "每个已知模块种类必须有独立的 session 创建 scope")
    }

    /**
     * 阶段指标名由阶段名拼出，必须逐个落在根 scope 下，且带上阶段名。
     */
    @Test
    @OptIn(LLStatisticsOnlyApi::class)
    fun rawBuildAndMacroMetricNamesArePrefixedWithRoot() {
        CfirRawBuildSource.entries.forEach { source ->
            CfirRawBuildStage.entries.forEach { stage ->
                val expected = "${LLStatisticsScopes.name}.rawBuild.${source.metricSuffix}.${stage.metricSuffix}"
                assertEquals("$expected.duration", LLStatisticsMetricNames.rawBuildDuration(source, stage))
                assertEquals("$expected.runs", LLStatisticsMetricNames.rawBuildRuns(source, stage))
            }
        }
        CfirMacroConstructionStage.entries.forEach { stage ->
            val expected = "${LLStatisticsScopes.name}.macro.${stage.metricSuffix}"
            assertEquals("$expected.duration", LLStatisticsMetricNames.macroConstructionDuration(stage))
            assertEquals("$expected.runs", LLStatisticsMetricNames.macroConstructionRuns(stage))
        }
        assertEquals("${LLStatisticsScopes.name}.macro.expansion.files", LLStatisticsMetricNames.macroExpandFiles)
        assertEquals("${LLStatisticsScopes.name}.macro.expansion.surfaces", LLStatisticsMetricNames.macroExpandSurfaces)
        CfirMacroExpansionOutcome.entries.forEach { outcome ->
            assertEquals(
                "${LLStatisticsScopes.name}.macro.expansion.${outcome.metricSuffix}",
                LLStatisticsMetricNames.macroExpandOutcome(outcome),
            )
        }
    }

    /**
     * 阶段枚举与 scope 映射必须一一对应：漏登记某个阶段会让它既不记耗时也不记次数。
     */
    @Test
    fun everyMacroStageHasItsOwnScope() {
        val mapped = CfirMacroConstructionStage.entries.associateWith { LLStatisticsScopes.Macro.stage(it).name }
        assertEquals(CfirMacroConstructionStage.entries.size, mapped.values.toSet().size, "每个宏阶段必须有独立 scope")
        mapped.forEach { (stage, name) ->
            assertEquals("${LLStatisticsScopes.name}.macro.${stage.metricSuffix}", name)
        }
    }

    /**
     * 阶段指标名由阶段名拼出，必须逐个落在根 scope 下，且带上阶段名。
     */
    @Test
    @OptIn(LLStatisticsOnlyApi::class)
    fun phaseMetricNamesArePrefixedWithRootAndCarryPhaseName() {
        CfirResolvePhase.entries.filterNot { it.noProcessor }.forEach { phase ->
            val expectedSuffix = ".resolve.phases.${phase.name.lowercase()}."
            listOf(
                LLStatisticsMetricNames.resolvePhaseDuration(phase),
                LLStatisticsMetricNames.resolvePhaseFiles(phase),
                LLStatisticsMetricNames.resolvePhaseDeclarations(phase),
                LLStatisticsMetricNames.resolvePhaseRuns(phase),
            ).forEach { name ->
                assertTrue(name.startsWith("${LLStatisticsScopes.name}$expectedSuffix"), "阶段指标 $name 命名不符")
            }
        }
    }

}
