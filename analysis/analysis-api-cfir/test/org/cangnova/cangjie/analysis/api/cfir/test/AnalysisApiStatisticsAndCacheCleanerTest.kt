
@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.statistics.CaFirStatisticsService
import org.cangnova.cangjie.analysis.api.cfir.utils.CaCfirCacheCleaner
import org.cangnova.cangjie.analysis.api.platform.statistics.CaStatisticsService
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Analysis API 统计接线与强制缓存清理器的行为测试。
 *
 * 这组用例锁定与 Kotlin K2 对齐的三条接线契约：
 * 1. `CaStatisticsService` 由 CFIR 模块注册实现，统计开关默认关闭时不暴露实例；
 * 2. `CaCfirCacheCleaner` 由 CFIR 模块注册实现，取到的不是空操作实现；
 * 3. 清理调度在没有进行中分析时立即清空 session 缓存，有进行中分析时推迟到最后一个分析块退出。
 */
class AnalysisApiStatisticsAndCacheCleanerTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/statistics",
) {
    /**
     * 使用 standalone CFIR 配置运行统计与缓存清理接线测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证统计服务实现已注册，且统计开关默认关闭时不对外暴露实例。
     */
    @Test
    fun statisticsServiceRegistered(mainFile: CjFile) {
        val project = mainFile.project

        assertTrue(
            project.getService(CaStatisticsService::class.java) is CaFirStatisticsService,
            "CFIR Analysis API 必须把 CaStatisticsService 注册为 CaFirStatisticsService。",
        )
        assertNull(
            CaStatisticsService.getInstance(project),
            "统计开关默认关闭时 CaStatisticsService.getInstance 必须返回 null。",
        )
        // 没有 low-level 统计服务（无 OpenTelemetry 实例）时 start() 必须是空操作。
        CaFirStatisticsService(project).start()
    }

    /**
     * 验证缓存清理器服务已注册，`getInstance` 取到的就是该实现而不是空操作实现。
     */
    @Test
    fun cacheCleanerServiceRegistered(mainFile: CjFile) {
        val project = mainFile.project

        assertSame(
            project.getService(CaCfirCacheCleaner::class.java),
            CaCfirCacheCleaner.getInstance(project),
            "CaCfirCacheCleaner.getInstance 必须返回 XML 注册的 stop-the-world 实现。",
        )
    }

    /**
     * 验证没有进行中分析时，清理调度立即生效：随后进入分析拿到的是全新 session。
     */
    @Test
    fun scheduleCleanupInvalidatesSessions(mainFile: CjFile) {
        val project = mainFile.project

        val sessionBefore = analyzeForTest(mainFile) { this }
        CaCfirCacheCleaner.getInstance(project).scheduleCleanup()
        val sessionAfter = analyzeForTest(mainFile) { this }

        assertNotSame(sessionBefore, sessionAfter, "清理完成后必须重新创建 Analysis API session。")
    }

    /**
     * 验证存在进行中分析时清理被推迟，直到最后一个分析块退出才执行。
     */
    @Test
    fun scheduleCleanupDeferredUntilAnalysisCompletes(mainFile: CjFile) {
        val project = mainFile.project
        val cleaner = CaCfirCacheCleaner.getInstance(project)

        val sessionBefore = analyzeForTest(mainFile) { this }

        cleaner.enterAnalysis()
        try {
            cleaner.scheduleCleanup()

            val sessionDuringAnalysis: CaSession = analyzeForTest(mainFile) { this }
            assertSame(sessionBefore, sessionDuringAnalysis, "存在进行中分析时不得清理 session 缓存。")
        } finally {
            cleaner.exitAnalysis()
        }

        val sessionAfter = analyzeForTest(mainFile) { this }
        assertNotSame(sessionBefore, sessionAfter, "最后一个分析块退出后挂起的清理必须立即执行。")
    }
}
