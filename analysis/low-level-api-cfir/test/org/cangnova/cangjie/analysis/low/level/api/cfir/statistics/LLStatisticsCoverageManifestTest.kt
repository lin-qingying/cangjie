package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import org.cangnova.cangjie.parsing.CangjiePsiParseKind
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 统计覆盖清单守卫：**每个埋点目标都必须登记负责验证它的用例。**
 *
 * 存在的理由：观察者接口的单测与统计域到指标名的映射测试都绿，只证明两端存在，不证明接缝
 * 通了。PSI 解析就栽在这里——指标恒为 0，只有加诊断才发现没有任何东西到达接缝。于是这里要求
 * 每个目标在清单里登记一条"负责它的用例"，新增埋点目标忘了登记时本测试立刻变红。
 *
 * `reachable` 区分两种情况：
 *
 * - `true`：真实宿主路径上能产生采样，清单里的用例走真实路径。
 * - `false`：分析宿主上不触发（分析宿主不跑宏构造、没有库模块、没有 IDE 补全路径），由该通路
 *   所属模块自己的 seam 测试负责。清单里必须写明理由，否则这里失败。
 *
 * 本测试跑在 low-level 模块内，因此能看见 internal 的 [LLStatisticsScopes]；性能测试模块只
 * 能看到公开的 [LLStatisticsMetricNames]，做不了这个对账。
 */
class LLStatisticsCoverageManifestTest {
    /**
     * scope 清单必须与 [LLStatisticsScopes] 的实际结构完全对账：不多不少。
     */
    @Test
    fun everyScopeIsRegisteredInTheManifest() {
        val registered = SCOPE_MANIFEST.keys
        val missing = ALL_SCOPES.filterNot { registered.contains(it) }
        assertTrue(
            missing.isEmpty(),
            "以下 scope 未登记覆盖清单，新增埋点目标必须同步登记：" +
                "\n" + missing.joinToString("\n") { "  ${it.name}" },
        )
        val stale = registered.filterNot { ALL_SCOPES.contains(it) }
        assertTrue(
            stale.isEmpty(),
            "清单里登记了不存在的 scope（scope 已改名或删除）：" +
                "\n" + stale.joinToString("\n") { "  ${it.name}" },
        )
    }

    /**
     * PSI 解析的每个入口 kind 都必须登记——它按 kind 生成指标名而不是嵌套 scope，
     * 所以单独按枚举对账。
     */
    @Test
    fun everyPsiParseKindIsRegistered() {
        val missing = CangjiePsiParseKind.entries.filterNot { PSI_PARSE_KIND_MANIFEST.containsKey(it) }
        assertTrue(
            missing.isEmpty(),
            "以下 PSI 解析入口未登记覆盖清单：" +
                "\n" + missing.joinToString("\n") { "  ${it.metricSuffix}" },
        )
        val stale = PSI_PARSE_KIND_MANIFEST.keys.filterNot { CangjiePsiParseKind.entries.contains(it) }
        assertTrue(
            stale.isEmpty(),
            "清单里登记了不存在的解析入口（kind 已改名或删除）：" +
                "\n" + stale.joinToString("\n") { "  ${it.metricSuffix}" },
        )
    }

    /**
     * 每条登记都必须写明负责它的用例与可达性，不允许空占位。
     */
    @Test
    fun everyManifestEntryNamesItsTestAndJustifiesUnreachability() {
        (SCOPE_MANIFEST.values + PSI_PARSE_KIND_MANIFEST.values).forEach { entry ->
            assertTrue(entry.verifiedBy.isNotBlank(), "未写明负责验证它的用例")
            if (!entry.reachableInAnalysisHost) {
                assertTrue(
                    entry.unreachableReason.isNotBlank(),
                    "在分析宿主上不可达的目标必须写明理由：${entry.verifiedBy}",
                )
            }
        }
    }

    /**
     * 可达条目不得为 0，否则说明整份清单都标成不可达，清单本身就失去意义。
     */
    @Test
    fun manifestHasReachableEntries() {
        val all = SCOPE_MANIFEST.values + PSI_PARSE_KIND_MANIFEST.values
        assertTrue(all.any { it.reachableInAnalysisHost }, "至少要有目标能在分析宿主上产生采样")
    }

    /**
     * 清单必须落在仓颉自己的命名空间下。
     */
    @Test
    fun manifestIsNamespaced() {
        SCOPE_MANIFEST.keys.forEach { scope ->
            assertTrue(
                scope.name.startsWith("${LLStatisticsScopes.name}."),
                "scope ${scope.name} 必须以根 scope ${LLStatisticsScopes.name} 为前缀",
            )
        }
    }

    /**
     * 全部 scope 清单。改动 [LLStatisticsScopes] 的结构时这里必须同步，否则对账会失败。
     */
    private val ALL_SCOPES: List<LLStatisticsScope> = listOf(
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
        LLStatisticsScopes.Diagnostics.CheckerComponent,
        LLStatisticsScopes.SessionCreation,
        LLStatisticsScopes.Scopes,
        LLStatisticsScopes.Parse,
        LLStatisticsScopes.Deserialization,
        LLStatisticsScopes.Deserialization.ClassLike,
        LLStatisticsScopes.Deserialization.Cjo,
        LLStatisticsScopes.Deserialization.Cjo.PackageLoad,
        LLStatisticsScopes.Deserialization.Cjo.Declaration,
        LLStatisticsScopes.SymbolProviders,
        LLStatisticsScopes.SymbolProviders.Combined,
        LLStatisticsScopes.SymbolProviders.Combined.Classes,
        LLStatisticsScopes.SymbolProviders.Combined.Classes.Hits,
        LLStatisticsScopes.SymbolProviders.Combined.Classes.Misses,
        LLStatisticsScopes.SymbolProviders.Combined.Callables,
        LLStatisticsScopes.SymbolProviders.Combined.Callables.Hits,
        LLStatisticsScopes.SymbolProviders.Combined.Callables.Misses,
    )

    /**
     * 一条覆盖登记。
     *
     * @param verifiedBy 负责验证该目标的用例（文件或用例名）
     * @param reachableInAnalysisHost 分析宿主真实路径上能否产生采样
     * @param unreachableReason 不可达时的理由，可达时留空
     */
    private data class Coverage(
        val verifiedBy: String,
        val reachableInAnalysisHost: Boolean,
        val unreachableReason: String = "",
    )

    private companion object {
        /**
         * scope 级覆盖清单。新增 scope 必须在这里登记。
         */
        val SCOPE_MANIFEST: Map<LLStatisticsScope, Coverage> = buildMap {
            put(LLStatisticsScopes.AnalysisSessions, Coverage("CaAnalysisApiStatisticsTest", true))
            put(LLStatisticsScopes.AnalysisSessions.Analyze, Coverage("CaAnalysisApiStatisticsTest", true))
            put(
                LLStatisticsScopes.AnalysisSessions.Analyze.Invocations,
                Coverage("CaAnalysisApiStatisticsTest#analyzeInvocationCounterIsRecorded", true),
            )
            put(
                LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup,
                Coverage("CaAnalysisApiStatisticsTest#lowMemoryCacheCleanupCounterIsRecorded", true),
            )
            put(
                LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup.Invocations,
                Coverage("CaAnalysisApiStatisticsTest#lowMemoryCacheCleanupCounterIsRecorded", true),
            )
            listOf(
                LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache,
                LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Hits,
                LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Misses,
                LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.Evictions,
                LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache,
                LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache,
            ).forEach { put(it, Coverage("CaAnalysisApiStatisticsTest", true)) }

            put(LLStatisticsScopes.Resolve, Coverage("CaResolvePhaseTimingTest（真实 collectDiagnostics 路径）", true))
            put(
                LLStatisticsScopes.Resolve.Phases,
                Coverage("CaResolvePhaseTimingTest#phaseDurationsAreRecordedForLazyResolve", true),
            )

            put(LLStatisticsScopes.RawBuild, Coverage("CaRawBuildStatisticsTest（PSI 路径真实驱动）", true))

            val macroReason = "分析宿主按设计不运行宏构造：宏只在 compiler/frontend 前端与 LSP 路径展开"
            put(LLStatisticsScopes.Macro, Coverage("MacroConstructionTimingObserverTest（cfir/analysis-tests，真实 construction）", false, macroReason))
            listOf(
                LLStatisticsScopes.Macro.SymbolIndex,
                LLStatisticsScopes.Macro.ImportBinding,
                LLStatisticsScopes.Macro.Expansion,
            ).forEach { put(it, Coverage("MacroConstructionTimingObserverTest#allThreeConstructionStagesAreReported", false, macroReason)) }

            put(LLStatisticsScopes.Diagnostics, Coverage("CaDiagnosticsStatisticsTest", true))
            listOf(
                LLStatisticsScopes.Diagnostics.Collection,
                LLStatisticsScopes.Diagnostics.ElementCollection,
                LLStatisticsScopes.Diagnostics.StructureBuild,
                LLStatisticsScopes.Diagnostics.StructureElement,
                LLStatisticsScopes.Diagnostics.Pass,
            ).forEach { put(it, Coverage("CaDiagnosticsStatisticsTest#diagnosticsCollectionStagesAreRecorded", true)) }
            put(
                LLStatisticsScopes.Diagnostics.CheckerComponent,
                Coverage("CaDiagnosticsStatisticsTest#checkerComponentTimingsAreRecorded", true),
            )

            put(LLStatisticsScopes.SessionCreation, Coverage("CaSessionAndDeserializationStatisticsTest", true))
            put(LLStatisticsScopes.Scopes, Coverage("CaSessionAndDeserializationStatisticsTest#sessionCreationAndDeserializationAreCounted", true))

            put(LLStatisticsScopes.Parse, Coverage("CaParserStatisticsTest（PSI 解析入口按 kind 埋点）", true))

            put(
                LLStatisticsScopes.Deserialization,
                Coverage("CjoDeserializationTimingSeamTest + CaSessionAndDeserializationStatisticsTest", true),
            )
            put(
                LLStatisticsScopes.Deserialization.ClassLike,
                Coverage(
                    "CaSessionAndDeserializationStatisticsTest（域映射）",
                    false,
                    "分析宿主的 testData 没有库模块，stub classLike 反序列化不触发；IDE 打开含库工程时触发",
                ),
            )
            put(LLStatisticsScopes.Deserialization.Cjo, Coverage("CjoDeserializationTimingSeamTest（真实 SDK fixture）", true))
            listOf(
                LLStatisticsScopes.Deserialization.Cjo.PackageLoad,
                LLStatisticsScopes.Deserialization.Cjo.Declaration,
            ).forEach { put(it, Coverage("CjoDeserializationTimingSeamTest#realLibraryQueryReachesTheObserverThroughBothStages", true)) }

            put(LLStatisticsScopes.SymbolProviders, Coverage("CaAnalysisApiStatisticsTest", true))
            put(LLStatisticsScopes.SymbolProviders.Combined, Coverage("CaAnalysisApiStatisticsTest", true))
            listOf(
                LLStatisticsScopes.SymbolProviders.Combined.Classes,
                LLStatisticsScopes.SymbolProviders.Combined.Classes.Hits,
                LLStatisticsScopes.SymbolProviders.Combined.Classes.Misses,
                LLStatisticsScopes.SymbolProviders.Combined.Callables,
                LLStatisticsScopes.SymbolProviders.Combined.Callables.Hits,
                LLStatisticsScopes.SymbolProviders.Combined.Callables.Misses,
            ).forEach { put(it, Coverage("CaAnalysisApiStatisticsTest", true)) }
        }

        /**
         * PSI 解析入口级覆盖清单：它按 kind 生成指标名，没有嵌套 scope，所以单独按枚举对账。
         */
        val PSI_PARSE_KIND_MANIFEST: Map<CangjiePsiParseKind, Coverage> = mapOf(
            CangjiePsiParseKind.FILE to
                Coverage("CaParserStatisticsTest#psiFileParseDurationIsRecorded", true),
            CangjiePsiParseKind.BLOCK_EXPRESSION to
                Coverage("CaParserStatisticsTest 随真实负载覆盖", true),
            CangjiePsiParseKind.LAMBDA_EXPRESSION to
                Coverage(
                    "LLStatisticsScopesTest#everyPsiParseKindHasItsOwnScope（名字层）",
                    false,
                    "lambda 解析只由 IDE 补全路径触发，分析宿主的 collectDiagnostics 不解析 lambda",
                ),
            CangjiePsiParseKind.BLOCK_CODE_FRAGMENT to
                Coverage(
                    "LLStatisticsScopesTest#everyPsiParseKindHasItsOwnScope（名字层）",
                    false,
                    "code fragment 只由 IDE 补全路径触发，分析宿主不走",
                ),
            CangjiePsiParseKind.EXPRESSION_CODE_FRAGMENT to
                Coverage(
                    "LLStatisticsScopesTest#everyPsiParseKindHasItsOwnScope（名字层）",
                    false,
                    "code fragment 只由 IDE 补全路径触发，分析宿主不走",
                ),
            CangjiePsiParseKind.TYPE_CODE_FRAGMENT to
                Coverage(
                    "LLStatisticsScopesTest#everyPsiParseKindHasItsOwnScope（名字层）",
                    false,
                    "code fragment 只由 IDE 补全路径触发，分析宿主不走",
                ),
        )
    }
}