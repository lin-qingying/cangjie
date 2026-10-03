package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserProfiler
import org.cangnova.cangjie.phaser.invokeToplevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * 真实编译的阶段耗时报告必须包含 cjo 写出阶段。
 *
 * 这条断言是 cjo 写侧埋点的全部意义所在：写出耗时此前被并进前端阶段，报告里既看不到它，
 * 写失败也只表现为"前端阶段失败"。[FrontendPipelineCompositionTest] 只证明机制通，这里证明
 * 真实编译确实走通了机制。
 */
@OptIn(CompilerConfiguration.Internals::class)
class CjoWritePhaseProfilingTest {
    /**
     * 走 phaser 协议跑一次真实编译，报告里前端阶段与 cjo 写出阶段各自独立成行。
     */
    @Test
    fun cjoWritePhaseIsProfiledSeparatelyFromTheFrontendPhase(@TempDir tempDir: Path) {
        val source = Files.writeString(
            tempDir.resolve("library.cj"),
            """
            package sample.library

            func identity(value: Int64): Int64 {
                return value
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )
        val outputRoot = Files.createDirectories(tempDir.resolve("output"))
        val disposable = Disposer.newDisposable("cjo-write-phase-profile")
        try {
            val collector = RecordingMessageCollector()
            val configuration = CompilerConfiguration().apply {
                moduleName = "cjo_write_profile"
                messageCollector = collector
                diagnosticsCollector = DiagnosticsCollectorImpl()
                addCangJieSourceRoot(source.toString())
                cjoOutputDirectory = outputRoot.toString()
            }
            val profiler = PhaserProfiler()

            cfirFrontendPipeline().invokeToplevel(
                PhaseConfig(profiler = profiler),
                PipelineContext(configuration),
                ConfigurationPipelineArtifact(configuration, disposable),
            )

            val names = profiler.timings().map { it.name }
            assertTrue(names.contains("CfirFrontendPipelinePhase"), "报告缺少前端阶段，实际：$names")
            assertTrue(names.contains("CjoWritePipelinePhase"), "报告缺少 cjo 写出阶段，实际：$names")
            assertTrue(
                profiler.timings().all { it.completed },
                "两个阶段都应正常完成，实际：${profiler.timings()}",
            )
            assertEquals(0, collector.errors.size, "真实编译不应报错：${collector.errors}")
            assertTrue(
                Files.walk(outputRoot).use { paths -> paths.anyMatch(Files::isRegularFile) },
                "cjo 写出阶段必须真的产出文件",
            )
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /**
     * 记录消息的 [MessageCollector] 替身。
     */
    private class RecordingMessageCollector : MessageCollector {
        val errors = mutableListOf<String>()

        override fun clear() {
            errors.clear()
        }

        override fun hasErrors(): Boolean = errors.isNotEmpty()

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            if (severity.isError) errors += message
        }
    }
}
