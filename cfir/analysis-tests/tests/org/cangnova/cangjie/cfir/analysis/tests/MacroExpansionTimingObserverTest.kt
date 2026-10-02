package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionTimingObserver
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.registerMacroExpansionTimingObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * macro construction 耗时观察者的 seam 测试。
 *
 * 验证 `expandWithDefaultContext`（identity 路径，与前端真实 construction 同一包装）会把
 * 结果归类、文件数与 surface 数交给观察者，且没有 surface 时 surface 数为 0。
 */
class MacroExpansionTimingObserverTest : AbstractCfirAnalysisTestCase() {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * identity construction 成功后观察者收到 SUCCESS 事件。
     */
    @Test
    fun identityConstructionReportsSuccess() {
        val session = createTestSession()
        val observer = RecordingMacroExpansionObserver()
        session.registerMacroExpansionTimingObserver(observer)

        createCjFile("main", "main() {}").toCfirFile(session)

        val event = observer.events.single()
        assertEquals(MacroConstructionService.Mode.STRICT, event.mode)
        assertEquals(CfirMacroExpansionOutcome.SUCCESS, event.outcome)
        assertEquals(1, event.fileCount)
        assertEquals(0, event.surfaceCount)
        assertTrue(event.elapsedNanos >= 0)
    }

    /**
     * 未注册观察者时 construction 照常完成。
     */
    @Test
    fun constructionSucceedsWithoutObserver() {
        val file = createCjFile("main", "main() {}").toCfirFile(createTestSession())
        assertTrue(file.name.endsWith("main.cj"))
    }

    /**
     * 一次 construction 的观察事件。
     */
    private data class MacroExpansionEvent(
        val mode: MacroConstructionService.Mode,
        val outcome: CfirMacroExpansionOutcome,
        val fileCount: Int,
        val surfaceCount: Int,
        val elapsedNanos: Long,
    )

    /**
     * 记录回调的观察者替身。
     */
    private class RecordingMacroExpansionObserver : CfirMacroExpansionTimingObserver {
        val events: MutableList<MacroExpansionEvent> = mutableListOf()

        override fun onMacroExpansionFinished(
            mode: MacroConstructionService.Mode,
            outcome: CfirMacroExpansionOutcome,
            fileCount: Int,
            surfaceCount: Int,
            elapsedNanos: Long,
        ) {
            events.add(MacroExpansionEvent(mode, outcome, fileCount, surfaceCount, elapsedNanos))
        }
    }
}
