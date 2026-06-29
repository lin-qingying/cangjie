package org.cangnova.cangjie.frontend.sources

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.frontend.environment.VfsBasedProjectEnvironment
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * 覆盖前端源文件收集的 source root 去重和空输入行为。
 */
class CjSourceCollectionTest {
    /**
     * 每个测试使用的临时源文件根目录。
     */
    @TempDir
    lateinit var tempDir: Path

    /**
     * 验证重复 source root 只收集一次并报告强警告。
     */
    @Test
    fun duplicateRootsAreDeduplicated() {
        val sourceDir = tempDir.resolve("src").toFile().apply { mkdirs() }
        File(sourceDir, "sample.cj").writeText("func main() {}")

        val collector = RecordingMessageCollector()
        val configuration = CompilerConfiguration.createForCfirFrontend(messageCollector = collector)
        configuration.addCangJieSourceRoot(sourceDir.absolutePath)
        configuration.addCangJieSourceRoot(sourceDir.absolutePath)

        val environment = createEnvironment()
        val collected = collectCjSources(configuration, environment)

        assertEquals(1, collected.allSources.size)
        assertTrue(
            collector.messages.any { it.severity == CompilerMessageSeverity.STRONG_WARNING },
            "Expected duplicate root warning",
        )
    }

    /**
     * 验证没有配置 source root 时返回空源文件集合。
     */
    @Test
    fun emptyRootsYieldNoSources() {
        val collector = RecordingMessageCollector()
        val configuration = CompilerConfiguration.createForCfirFrontend(messageCollector = collector)
        val environment = createEnvironment()

        val collected = collectCjSources(configuration, environment)

        assertEquals(0, collected.allSources.size)
    }

    /**
     * 创建带本地和 jar 文件系统的 VFS 项目环境。
     */
    private fun createEnvironment(): VfsBasedProjectEnvironment {
        val coreEnvironment = CangJieCoreEnvironment.create(Disposable { }, CangJieCoreEnvironmentMode.Production)
        val project = coreEnvironment.projectEnvironment.project
        return VfsBasedProjectEnvironment(
            project = project,
            knownFileSystems = listOf(StandardFileSystems.local(), StandardFileSystems.jar()),
        )
    }

    /**
     * 记录测试中 message collector 收到的消息。
     */
    private class RecordingMessageCollector : MessageCollector {
        /**
         * 单条编译消息记录。
         */
        data class Entry(val severity: CompilerMessageSeverity, val message: String)

        /**
         * 已记录的消息列表。
         */
        val messages = mutableListOf<Entry>()

        /**
         * 清空已记录消息。
         */
        override fun clear() = messages.clear()

        /**
         * 记录一次编译消息。
         */
        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            messages += Entry(severity, message)
        }

        /**
         * 判断记录中是否包含错误级消息。
         */
        override fun hasErrors(): Boolean = messages.any { it.severity.isError }
    }
}
