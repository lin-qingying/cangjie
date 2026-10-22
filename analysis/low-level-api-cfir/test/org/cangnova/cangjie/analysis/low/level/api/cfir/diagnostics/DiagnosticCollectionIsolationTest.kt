package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 诊断收集的异常隔离契约。
 *
 * [forEachWithDiagnosticIsolation] 是 `FileStructure.collectDiagnosticsFromStructureElements`
 * 实际使用的 element 级收集入口；该边界必须把单个元素的失败记录下来并继续，而不是让平台
 * logger 把异常升级为 AssertionError、终止整份文件的收集。
 */
class DiagnosticCollectionIsolationTest {
    /** 单个 checker/element 抛出的异常不得逃出隔离边界，且必须可被观测。 */
    @Test
    fun failingElementDoesNotEscape() {
        DiagnosticCollectionFailures.clear()
        assertNull(
            LLCfirDiagnosticVisitor.suppressAndLogExceptions<Unit> {
                throw IllegalStateException("simulated checker failure")
            },
        )
        assertEquals(1, DiagnosticCollectionFailures.drain().size)
    }

    /** 一个元素失败后，后续元素仍必须继续被收集，隔离异常可被逐元素观测。 */
    @Test
    fun laterElementsContinueAfterFailure() {
        DiagnosticCollectionFailures.clear()
        val collected = mutableListOf<String>()
        forEachWithDiagnosticIsolation(listOf("broken", "healthy")) { name ->
            if (name == "broken") {
                throw IllegalStateException("simulated checker failure")
            }
            collected += name
        }

        assertEquals(listOf("healthy"), collected)
        val failures = DiagnosticCollectionFailures.drain()
        assertEquals(1, failures.size)
        assertTrue(failures.single().message.orEmpty().contains("simulated checker failure"))
    }
}
