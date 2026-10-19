package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.enableChirJvmCodegen
import org.cangnova.cangjie.config.jvmClassOutputDirectory
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
 * CHIR → JVM codegen 管线阶段的集成测试。
 *
 * 走真实编译验证三件事：阶段出现在耗时报告中、class 文件真实产出、
 * 配置关闭时该阶段是空操作（不出报告也不产文件）。
 */
@OptIn(CompilerConfiguration.Internals::class)
class ChirJvmCodegenPipelinePhaseTest {
    /**
     * 开启 `--emit-jvm` 的真实编译：报告包含 codegen 阶段，输出目录有 class 文件。
     */
    @Test
    fun emitJvmWritesClassesThroughTheRealPipeline(@TempDir tempDir: Path) {
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
        val outputRoot = Files.createDirectories(tempDir.resolve("classes"))
        val disposable = Disposer.newDisposable("chir-jvm-codegen-pipeline")
        try {
            val collector = RecordingMessageCollector()
            val configuration = CompilerConfiguration().apply {
                moduleName = "chir_jvm_codegen"
                messageCollector = collector
                diagnosticsCollector = DiagnosticsCollectorImpl()
                addCangJieSourceRoot(source.toString())
                enableChirJvmCodegen = true
                jvmClassOutputDirectory = outputRoot.toString()
            }
            val profiler = PhaserProfiler()

            cfirFrontendPipeline().invokeToplevel(
                PhaseConfig(profiler = profiler),
                PipelineContext(configuration),
                ConfigurationPipelineArtifact(configuration, disposable),
            )

            val names = profiler.timings().map { it.name }
            assertTrue(names.contains("ChirJvmCodegenPipelinePhase"), "报告缺少 codegen 阶段，实际：$names")
            assertEquals(0, collector.errors.size, "编译不应报错：${collector.errors}")
            val classFiles = Files.walk(outputRoot).use { paths ->
                paths.filter(Files::isRegularFile).filter { it.toString().endsWith(".class") }.toList()
            }
            assertTrue(classFiles.isNotEmpty(), "codegen 阶段必须产出 class 文件")
            // facade 命名策略：模块名 Library 后缀 Cj。
            assertTrue(
                classFiles.any { it.fileName.toString().endsWith("LibraryCj.class") },
                "应产出模块 facade class，实际：$classFiles",
            )
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /**
     * 配置关闭时 codegen 阶段是空操作：不产文件，报告里阶段仍执行（明确空操作）。
     */
    @Test
    fun disabledCodegenProducesNoClasses(@TempDir tempDir: Path) {
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
        val outputRoot = Files.createDirectories(tempDir.resolve("classes"))
        val disposable = Disposer.newDisposable("chir-jvm-codegen-disabled")
        try {
            val collector = RecordingMessageCollector()
            val configuration = CompilerConfiguration().apply {
                moduleName = "chir_jvm_codegen_disabled"
                messageCollector = collector
                diagnosticsCollector = DiagnosticsCollectorImpl()
                addCangJieSourceRoot(source.toString())
                // 不开 enableChirJvmCodegen。
            }
            val profiler = PhaserProfiler()

            cfirFrontendPipeline().invokeToplevel(
                PhaseConfig(profiler = profiler),
                PipelineContext(configuration),
                ConfigurationPipelineArtifact(configuration, disposable),
            )

            assertEquals(0, collector.errors.size, "编译不应报错：${collector.errors}")
            val classFiles = Files.walk(outputRoot).use { paths ->
                paths.filter(Files::isRegularFile).filter { it.toString().endsWith(".class") }.toList()
            }
            assertEquals(emptyList<Path>(), classFiles, "关闭 codegen 时不应产出任何 class 文件")
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
