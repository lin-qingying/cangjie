package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroConstructionStage
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroConstructionTimingObserver
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.registerMacroConstructionTimingObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * macro construction 耗时观察者的 seam 测试。
 *
 * `toCfirFile` 走的是 identity construction，但它经由 `expandWithDefaultContext`，
 * 与前端真实路径共用同一段主流程（索引 → 绑定 → 展开）。因此这里能端到端验证三段
 * 全部上报，而不只是展开一段——只埋展开一步会让"卡在索引构建"被误读成"展开很快"。
 */
class MacroConstructionTimingObserverTest : AbstractCfirAnalysisTestCase() {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * identity construction 的三段全部上报，展开段带 SUCCESS 归类与工作量。
     */
    @Test
    fun allThreeConstructionStagesAreReported() {
        val session = createTestSession()
        val observer = RecordingMacroConstructionObserver()
        session.registerMacroConstructionTimingObserver(observer)

        createCjFile("main", "main() {}").toCfirFile(session)

        // 索引与绑定必须成对出现：少任何一段都说明 construction 主流程只埋了展开一步。
        // 次数可以多于一次——库/builtins session 创建本身也会走一遍 construction。
        val indexCount = observer.stages.count { it == CfirMacroConstructionStage.SYMBOL_INDEX }
        val bindingCount = observer.stages.count { it == CfirMacroConstructionStage.IMPORT_BINDING }
        assertTrue("符号索引构建必须被计时", indexCount >= 1)
        assertEquals("import 绑定与索引构建必须成对", indexCount, bindingCount)

        val expansionIndices = observer.stages.indices.filter { observer.stages[it] == CfirMacroConstructionStage.EXPANSION }
        assertTrue("展开必须被计时", expansionIndices.isNotEmpty())
        assertEquals("本文件的 construction 必须以展开收尾", observer.stages.last(), CfirMacroConstructionStage.EXPANSION)

        assertTrue("每段都必须上报耗时", observer.elapsedNanos.size == observer.stages.size)
        assertTrue(observer.elapsedNanos.all { it >= 0L })

        // 索引与绑定两段没有结果归类，只有展开段有。
        assertTrue(observer.outcomes.filterIndexed { i, o -> o != null && observer.stages[i] != CfirMacroConstructionStage.EXPANSION }.isEmpty())
        val expansion = expansionIndices.first()
        assertEquals(CfirMacroExpansionOutcome.SUCCESS, observer.outcomes[expansion])
        assertEquals(MacroConstructionService.Mode.STRICT, observer.modes[expansion])
        assertEquals(1, observer.fileCounts[expansion])
        assertEquals(0, observer.surfaceCounts[expansion])
    }

    /**
     * 未注册观察者时 construction 照常完成（零开销路径）。
     */
    @Test
    fun constructionSucceedsWithoutObserver() {
        val file = createCjFile("main", "main() {}").toCfirFile(createTestSession())
        assertTrue(file.name.endsWith("main.cj"))
    }

    /**
     * 记录回调的观察者替身。
     */
    private class RecordingMacroConstructionObserver : CfirMacroConstructionTimingObserver {
        val stages: MutableList<CfirMacroConstructionStage> = mutableListOf()
        val modes: MutableList<MacroConstructionService.Mode?> = mutableListOf()
        val outcomes: MutableList<CfirMacroExpansionOutcome?> = mutableListOf()
        val fileCounts: MutableList<Int> = mutableListOf()
        val surfaceCounts: MutableList<Int> = mutableListOf()
        val elapsedNanos: MutableList<Long> = mutableListOf()

        override fun onMacroConstructionFinished(
            stage: CfirMacroConstructionStage,
            mode: MacroConstructionService.Mode?,
            outcome: CfirMacroExpansionOutcome?,
            fileCount: Int,
            surfaceCount: Int,
            elapsedNanos: Long,
        ) {
            stages.add(stage)
            modes.add(mode)
            outcomes.add(outcome)
            fileCounts.add(fileCount)
            surfaceCounts.add(surfaceCount)
            this.elapsedNanos.add(elapsedNanos)
        }
    }
}
