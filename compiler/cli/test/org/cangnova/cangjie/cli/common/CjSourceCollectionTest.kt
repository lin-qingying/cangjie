package org.cangnova.cangjie.cli.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.cli.compiler.VfsBasedProjectEnvironment
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

class CjSourceCollectionTest {
    @TempDir
    lateinit var tempDir: Path

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

    @Test
    fun emptyRootsYieldNoSources() {
        val collector = RecordingMessageCollector()
        val configuration = CompilerConfiguration.createForCfirFrontend(messageCollector = collector)
        val environment = createEnvironment()

        val collected = collectCjSources(configuration, environment)

        assertEquals(0, collected.allSources.size)
    }

    private fun createEnvironment(): VfsBasedProjectEnvironment {
        val coreEnvironment = CangJieCoreEnvironment.create(Disposable { }, CangJieCoreEnvironmentMode.Production)
        val project = coreEnvironment.projectEnvironment.project
        return VfsBasedProjectEnvironment(
            project = project,
            knownFileSystems = listOf(StandardFileSystems.local(), StandardFileSystems.jar()),
        )
    }

    private class RecordingMessageCollector : MessageCollector {
        data class Entry(val severity: CompilerMessageSeverity, val message: String)

        val messages = mutableListOf<Entry>()

        override fun clear() = messages.clear()

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            messages += Entry(severity, message)
        }

        override fun hasErrors(): Boolean = messages.any { it.severity.isError }
    }
}
