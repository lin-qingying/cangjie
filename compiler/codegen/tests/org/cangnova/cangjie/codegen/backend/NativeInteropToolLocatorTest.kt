package org.cangnova.cangjie.codegen.backend

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class NativeInteropToolLocatorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `returns configured absolute path as is`() {
        val locator = NativeInteropToolLocator(workingDirectory = tempDir, env = emptyMap())
        val configured = tempDir.resolve("custom").resolve("interop-tool").toString()

        assertEquals(configured, locator.resolve(configured))
    }

    @Test
    fun `prefers environment variable when provided`() {
        val expected = tempDir.resolve("env-tool").toString()
        val locator = NativeInteropToolLocator(
            workingDirectory = tempDir,
            env = mapOf("CANGJIE_LLVM_INTEROP_TOOL" to expected),
        )

        assertEquals(expected, locator.resolve("cangjie-llvm-interop"))
    }

    @Test
    fun `finds tool in default build directory`() {
        val toolPath = tempDir.resolve("tools").resolve("cangjie-llvm-interop").resolve("build")
            .resolve(executableName("cangjie-llvm-interop"))
        toolPath.parent.createDirectories()
        toolPath.writeText("stub")

        val locator = NativeInteropToolLocator(workingDirectory = tempDir, env = emptyMap())
        assertEquals(toolPath.toString(), locator.resolve("cangjie-llvm-interop"))
    }

    private fun executableName(base: String): String {
        return if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "$base.exe" else base
    }
}

