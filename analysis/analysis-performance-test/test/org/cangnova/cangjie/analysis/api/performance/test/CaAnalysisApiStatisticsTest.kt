package org.cangnova.cangjie.analysis.api.performance.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.utils.CaCfirCacheCleaner
import org.cangnova.cangjie.analysis.api.platform.statistics.CaStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 性能/统计入口的接线测试。
 *
 * 这些用例不比较耗时，只验证"统计链路在 SDK 支撑下真的产生数据"：
 * 开关、服务、计数器、缓存统计、强制清理计数。任何一处断线都会在这里暴露，
 * 性能用例则可以放心地基于 [counterDelta] 做相对断言。
 */
class CaAnalysisApiStatisticsTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 统计开关打开后，`CaStatisticsService` 与 `LLStatisticsService` 都必须可用，且 `start()` 可调用。
     *
     * 对齐 IDE 的启动链路：`CaIdeStatisticsStartupActivity` → `CaStatisticsService.start()` → `LLStatisticsService.start()`。
     */
    @Test
    fun statisticsServicesAreAvailableAndStartable(mainFile: CjFile) {
        assertNotNull(
            CaStatisticsService.getInstance(project),
            "registry key cangjie.analysis.statistics 打开后必须能取到 CaStatisticsService。",
        )
        val lowLevelService = requireNotNull(LLStatisticsService.getInstance(project)) {
            "注册 OpenTelemetry provider 后必须能取到 LLStatisticsService。"
        }

        // scheduler 只驱动异步 gauge 域（当前为空实现），这里只验证启动链路不抛错。
        lowLevelService.start()
    }

    /**
     * 每次进入分析都必须让 `analyze.invocations` 递增。
     */
    @Test
    fun analyzeInvocationCounterIsRecorded(mainFile: CjFile) {
        val delta = counterDelta {
            analyzeForTest(mainFile) {
                assertNotNull(mainFile, "用例必须提供 main 文件。")
            }
        }

        assertTrue(
            counter(delta, LLStatisticsMetricNames.analyzeInvocations) >= 1,
            "进入分析后 ${LLStatisticsMetricNames.analyzeInvocations} 必须至少 +1，实际 delta=$delta。",
        )
    }

    /**
     * 同一调用表达式连续解析两次，第一次未命中、第二次命中，`resolveCallCache` 的 miss/hit 都要有数据。
     */
    @Test
    fun resolveCallCacheStatsAreRecorded(mainFile: CjFile) {
        val callExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .firstOrNull { it.text.startsWith("helper") }
            ?: error("测试数据中缺少 helper() 调用表达式。")

        val delta = counterDelta {
            analyzeForTest(callExpression) { callExpression.resolveToCall() }
            analyzeForTest(callExpression) { callExpression.resolveToCall() }
        }

        val hits = counter(delta, LLStatisticsMetricNames.resolveCallCacheHits)
        val misses = counter(delta, LLStatisticsMetricNames.resolveCallCacheMisses)
        assertTrue(misses >= 1, "首次解析必须记录 miss，delta=$delta。")
        assertTrue(hits >= 1, "重复解析必须命中缓存，delta=$delta。")
    }

    /**
     * 同一名称引用连续做元素级解析两次，第一次未命中、第二次命中，
     * `resolveSymbolCache` 的 miss/hit 都要有数据。
     */
    @Test
    fun resolveSymbolCacheStatsAreRecorded(mainFile: CjFile) {
        val reference = PsiTreeUtil.findChildrenOfType(mainFile, CjNameReferenceExpression::class.java)
            .firstOrNull { it.referencedName == "helper" }
            ?: error("测试数据中缺少 helper 名称引用。")

        val delta = counterDelta {
            analyzeForTest(reference) { reference.resolveSymbol() }
            analyzeForTest(reference) { reference.resolveSymbol() }
        }

        val hits = counter(delta, LLStatisticsMetricNames.resolveSymbolCacheHits)
        val misses = counter(delta, LLStatisticsMetricNames.resolveSymbolCacheMisses)
        assertTrue(misses >= 1, "首次元素级解析必须记录 miss，delta=$delta。")
        assertTrue(hits >= 1, "重复元素级解析必须命中缓存，delta=$delta。")
    }

    /**
     * 同一引用连续做引用级解析两次，`resolveToSymbolsCache` 的 miss/hit 都要有数据。
     */
    @Test
    fun resolveToSymbolsCacheStatsAreRecorded(mainFile: CjFile) {
        val reference = PsiTreeUtil.findChildrenOfType(mainFile, CjNameReferenceExpression::class.java)
            .firstOrNull { it.referencedName == "helper" }
            ?: error("测试数据中缺少 helper 名称引用。")

        val delta = counterDelta {
            analyzeForTest(reference) { reference.resolveToSymbols() }
            analyzeForTest(reference) { reference.resolveToSymbols() }
        }

        val hits = counter(delta, LLStatisticsMetricNames.resolveToSymbolsCacheHits)
        val misses = counter(delta, LLStatisticsMetricNames.resolveToSymbolsCacheMisses)
        assertTrue(misses >= 1, "首次引用级解析必须记录 miss，delta=$delta。")
        assertTrue(hits >= 1, "重复引用级解析必须命中缓存，delta=$delta。")
    }

    /**
     * 强制缓存清理执行后，`lowMemoryCacheCleanup.invocations` 必须递增。
     */
    @Test
    fun lowMemoryCacheCleanupCounterIsRecorded(mainFile: CjFile) {
        analyzeForTest(mainFile) { }

        val delta = counterDelta {
            CaCfirCacheCleaner.getInstance(project).scheduleCleanup()
        }

        assertTrue(
            counter(delta, LLStatisticsMetricNames.lowMemoryCacheCleanupInvocations) >= 1,
            "执行强制清理后 ${LLStatisticsMetricNames.lowMemoryCacheCleanupInvocations} 必须至少 +1，实际 delta=$delta。",
        )
    }

    /**
     * 所有指标都必须落在 `cangjie.analysis` 命名空间下，避免与同进程内 Kotlin Analysis API 指标混名。
     */
    @Test
    fun metricNamesUseCangjieNamespace(mainFile: CjFile) {
        analyzeForTest(mainFile) { }

        val names = counterDelta { }.keys + CaPerformanceTestTelemetry.collectLongCounters().keys
        assertTrue(names.isNotEmpty(), "统计开启后必须能读到至少一个指标。")
        val foreign = names.filterNot { LLStatisticsMetricNames.isCangjieAnalysisMetric(it) }
        assertTrue(foreign.isEmpty(), "指标必须以 ${LLStatisticsMetricNames.root} 为前缀，越界指标：$foreign。")
    }
}