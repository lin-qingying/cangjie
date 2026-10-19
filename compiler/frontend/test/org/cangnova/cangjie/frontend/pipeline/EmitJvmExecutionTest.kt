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
import org.cangnova.cangjie.phaser.PhaserState
import org.cangnova.cangjie.phaser.invokeToplevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * 真实仓颉程序经 `--emit-jvm` 的执行级验证。
 *
 * 编译一个含函数调用、局部变量、算术与分支的 main 函数，
 * 加载生成的 class 并反射执行 main 验证控制流与运算的正确性。
 */
@OptIn(CompilerConfiguration.Internals::class)
class EmitJvmExecutionTest {
    /**
     * 编译并执行 main：add(1, 2) * 3 > 5 时返回 9。
     */
    @Test
    fun compiledProgramExecutesThroughFunctionCallArithmeticAndBranch(@TempDir tempDir: Path) {
        val source = Files.writeString(
            tempDir.resolve("program.cj"),
            """
            package demo.program

            func add(a: Int64, b: Int64): Int64 {
                return a + b
            }

            func compute(a: Int64, b: Int64): Int64 {
                // 缺口 8（见 docs/cfir-gaps-for-cfir2chir.md）：整数字面量
                // 在 BODY_RESOLVE 后仍为 IDEAL_INT，无法进 CHIR；
                // 源码从参数算术出发完全回避字面量，覆盖函数调用、
                // 局部变量、乘法、比较与分支。
                let base = add(a, b)
                let x = add(base, base)
                let y = x * base
                if (y > base) {
                    return y
                }
                return x
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )
        val outputRoot = Files.createDirectories(tempDir.resolve("classes"))
        val disposable = Disposer.newDisposable("emit-jvm-execution")
        try {
            val collector = RecordingMessageCollector()
            val configuration = CompilerConfiguration().apply {
                moduleName = "emit_jvm_execution"
                messageCollector = collector
                diagnosticsCollector = DiagnosticsCollectorImpl()
                addCangJieSourceRoot(source.toString())
                enableChirJvmCodegen = true
                jvmClassOutputDirectory = outputRoot.toString()
            }
            val profiler = PhaserProfiler()

            runCatching {
                cfirFrontendPipeline().invokeToplevel(
                    PhaseConfig(profiler = profiler),
                    PipelineContext(configuration),
                    ConfigurationPipelineArtifact(configuration, disposable),
                )
            }.onFailure { pipelineFailure ->
                // phase 的 JvmCodegenException 分支已把 CHIR dump 带进消息收集器。
                throw AssertionError(
                    "管线执行失败：$pipelineFailure\n消息收集器内容：${collector.all.joinToString("\n")}",
                    pipelineFailure,
                )
            }

            assertEquals(0, collector.errors.size, "编译不应报错：${collector.errors}")

            val classFiles = Files.walk(outputRoot).use { paths ->
                paths.filter(Files::isRegularFile).filter { it.toString().endsWith(".class") }.toList()
            }
            assertTrue(classFiles.isNotEmpty(), "必须产出 class 文件")

            val loader = GeneratedClassLoader()
            val definedClasses = classFiles.associate { file ->
                val internalName = outputRoot.relativize(file).toString()
                    .removeSuffix(".class").replace('\\', '/')
                internalName to loader.define(internalName, Files.readAllBytes(file))
            }
            val facadeClass = requireNotNull(
                definedClasses.entries.singleOrNull { it.key.endsWith("ProgramCj") }?.value,
            ) { "未找到模块 facade，实际：${definedClasses.keys}" }

            // compute(2, 3)：base=5, x=10, y=50 > 5 → 返回 50。
            val computeResult = facadeClass
                .getMethod("compute", java.lang.Long.TYPE, java.lang.Long.TYPE)
                .invoke(null, 2L, 3L)
            assertEquals(50L, computeResult)

            // 独立验证 add 函数。
            val addResult = facadeClass
                .getMethod("add", java.lang.Long.TYPE, java.lang.Long.TYPE)
                .invoke(null, 20L, 22L)
            assertEquals(42L, addResult)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /**
     * 编译并执行类成员方法与 while 循环：Counter 类在循环中累加。
     *
     * `Counter.step(a, b)`：从 a 出发，`while (total < b)` 每轮 `total = total + a`，
     * 返回循环结束时的 total。a=3, b=10 → 3,6,9,12 → 返回 12。
     */
    @Test
    fun compiledProgramExecutesClassMethodAndWhileLoop(@TempDir tempDir: Path) {
        val source = Files.writeString(
            tempDir.resolve("counter.cj"),
            """
            package demo.counter

            class Counter {
                func step(from: Int64, until: Int64): Int64 {
                    var total = from
                    while (total < until) {
                        total = total + from
                    }
                    return total
                }
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )
        val outputRoot = Files.createDirectories(tempDir.resolve("classes"))
        val disposable = Disposer.newDisposable("emit-jvm-counter")
        try {
            val collector = RecordingMessageCollector()
            val configuration = CompilerConfiguration().apply {
                moduleName = "emit_jvm_counter"
                messageCollector = collector
                diagnosticsCollector = DiagnosticsCollectorImpl()
                addCangJieSourceRoot(source.toString())
                enableChirJvmCodegen = true
                jvmClassOutputDirectory = outputRoot.toString()
            }

            runCatching {
                cfirFrontendPipeline().invokeToplevel(
                    PhaseConfig(),
                    PipelineContext(configuration),
                    ConfigurationPipelineArtifact(configuration, disposable),
                )
            }.onFailure { pipelineFailure ->
                throw AssertionError(
                    "管线执行失败：$pipelineFailure\n消息收集器内容：${collector.all.joinToString("\n")}",
                    pipelineFailure,
                )
            }

            assertEquals(0, collector.errors.size, "编译不应报错：${collector.errors}")

            val classFiles = Files.walk(outputRoot).use { paths ->
                paths.filter(Files::isRegularFile).filter { it.toString().endsWith(".class") }.toList()
            }
            assertTrue(classFiles.isNotEmpty(), "必须产出 class 文件")

            val loader = GeneratedClassLoader()
            val definedClasses = classFiles.associate { file ->
                val internalName = outputRoot.relativize(file).toString()
                    .removeSuffix(".class").replace('\\', '/')
                internalName to loader.define(internalName, Files.readAllBytes(file))
            }
            val counterClass = requireNotNull(
                definedClasses.entries.singleOrNull { it.key.endsWith("Counter") }?.value,
            ) { "未找到 Counter 类，实际：${definedClasses.keys}" }

            val instance = counterClass.getConstructor().newInstance()
            // step(3, 10)：3 → 6 → 9 → 12（12 >= 10 停）→ 返回 12。
            val stepResult = counterClass
                .getMethod("step", java.lang.Long.TYPE, java.lang.Long.TYPE)
                .invoke(instance, 3L, 10L)
            assertEquals(12L, stepResult)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /**
     * 从内存 class 字节定义并加载生成类的 classloader。
     */
    private class GeneratedClassLoader : ClassLoader(EmitJvmExecutionTest::class.java.classLoader) {
        fun define(internalName: String, bytes: ByteArray): Class<*> {
            return defineClass(internalName.replace('/', '.'), bytes, 0, bytes.size)
        }
    }

    /**
     * 记录消息的 [MessageCollector] 替身。
     */
    private class RecordingMessageCollector : MessageCollector {
        val errors = mutableListOf<String>()
        val all = mutableListOf<String>()

        override fun clear() {
            errors.clear()
            all.clear()
        }

        override fun hasErrors(): Boolean = errors.isNotEmpty()

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            all += "[$severity] $message"
            if (severity.isError) errors += message
        }
    }
}
