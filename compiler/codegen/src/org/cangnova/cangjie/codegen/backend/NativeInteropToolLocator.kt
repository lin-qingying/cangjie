package org.cangnova.cangjie.codegen.backend

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

class NativeInteropToolLocator(
    private val workingDirectory: Path = Paths.get(System.getProperty("user.dir")),
    private val env: Map<String, String> = System.getenv(),
) {
    fun resolve(configuredTool: String): String {
        if (configuredTool.contains('/') || configuredTool.contains('\\') || configuredTool.contains(':')) {
            return configuredTool
        }

        val fromEnv = env["CANGJIE_LLVM_INTEROP_TOOL"]
        if (!fromEnv.isNullOrBlank()) {
            return fromEnv
        }

        val toolName = executableName(configuredTool)
        val candidates = listOf(
            workingDirectory.resolve("tools").resolve("cangjie-llvm-interop").resolve("build").resolve(toolName),
            workingDirectory.resolve("tools").resolve("cangjie-llvm-interop").resolve("build").resolve("Release").resolve(toolName),
            workingDirectory.resolve("tools").resolve("cangjie-llvm-interop").resolve("build").resolve("Debug").resolve(toolName),
            workingDirectory.resolve("tools").resolve("cangjie-llvm-interop").resolve("bin").resolve(toolName),
        )

        return candidates.firstOrNull { Files.exists(it) && it.isRegularFile() }?.toString() ?: configuredTool
    }

    private fun executableName(base: String): String {
        return if (isWindows() && !base.endsWith(".exe", ignoreCase = true)) "$base.exe" else base
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
}

