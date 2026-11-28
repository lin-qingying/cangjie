package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * 阶段耗时报告的输出通道测试。
 *
 * `--dump-perf` 的路径错误只应报诊断、不改变编译结果：性能报告是辅助产物。
 */
class PhaseProfileReportTest {
    /**
     * `--dump-perf` 指定路径时把报告写进该文件。
     */
    @Test
    fun dumpsReportToFile(@TempDir tempDir: Path) {
        val target = tempDir.resolve("phase-profile.txt")
        val collector = RecordingMessageCollector()

        writePhaseProfileReport("Top: 1.000 ms\n", target.toString(), reportToConsole = false, messageCollector = collector)

        assertEquals("Top: 1.000 ms\n", Files.readString(target))
        assertTrue(collector.diagnostics.isEmpty(), "正常写文件不应产生诊断")
    }

    /**
     * 目标路径不可写时只报 ERROR，不抛异常——报告写不出去不能让编译失败。
     */
    @Test
    fun unwritablePathIsReportedAsDiagnostic() {
        val collector = RecordingMessageCollector()

        writePhaseProfileReport(
            report = "Top: 1.000 ms\n",
            dumpPath = "/definitely/not/a/writable/dir/profile.txt",
            reportToConsole = false,
            messageCollector = collector,
        )

        assertEquals(1, collector.diagnostics.size)
        assertEquals(CompilerMessageSeverity.ERROR, collector.diagnostics.single().first)
    }

    /**
     * 既没有 dump 路径也不要求控制台输出时不产生任何副作用。
     */
    @Test
    fun noDestinationIsNoOp() {
        val collector = RecordingMessageCollector()
        writePhaseProfileReport("Top: 1.000 ms\n", dumpPath = null, reportToConsole = false, messageCollector = collector)
        assertTrue(collector.diagnostics.isEmpty())
    }

    /**
     * 记录诊断的 [MessageCollector] 替身。
     */
    private class RecordingMessageCollector : MessageCollector {
        val diagnostics = mutableListOf<Pair<CompilerMessageSeverity, String>>()

        override fun clear() {
            diagnostics.clear()
        }

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            diagnostics.add(severity to message)
        }

        override fun hasErrors(): Boolean = diagnostics.any { it.first.isError }
    }
}
