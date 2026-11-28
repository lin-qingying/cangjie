package org.cangnova.cangjie.analysis.realproject

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * 调用 SDK 自带的 cjpm，对真实项目执行 clean/build。
 *
 * cjpm 编译成功是后续 CFIR 分析的前提：项目本身没有错误时，
 * CFIR 报出的诊断/异常才可能是本仓库实现的问题。
 */
class CjpmRunner(
    private val config: RealProjectConfig,
    private val projectDir: Path,
) {
    /** 单次 cjpm 调用的结果。 */
    data class Result(
        val command: String,
        val exitCode: Int,
        val output: String,
        val durationMillis: Long,
    )

    /** 先 clean 再 build；返回两次调用的结果。 */
    fun build(): List<Result> = listOf(
        run(listOf("clean"), timeoutMinutes = 5),
        run(listOf("build"), timeoutMinutes = 20),
    )

    /** SDK 中需要出现在 PATH 上的目录：可执行文件 + 运行时库。 */
    private fun buildSdkPath(config: RealProjectConfig): String {
        val runtimeLib = config.cangjieHome.resolve("runtime").resolve("lib")
        val runtimeLibDirs = if (Files.isDirectory(runtimeLib)) {
            Files.list(runtimeLib).use { stream ->
                stream.filter { Files.isDirectory(it) }.toList()
            }
        } else {
            emptyList()
        }
        return (listOf(
            config.cangjieHome.resolve("bin"),
            config.cangjieHome.resolve("tools").resolve("bin"),
        ) + runtimeLibDirs)
            .filter(Files::isDirectory)
            .joinToString(java.io.File.pathSeparator) { it.toString() }
    }

    private fun run(args: List<String>, timeoutMinutes: Long): Result {
        val command = listOf(config.cjpmExecutable.toString()) + args
        check(Files.isExecutable(config.cjpmExecutable)) {
            "cjpm 不存在或不可执行: ${config.cjpmExecutable}"
        }

        val outputFile = Files.createTempFile("cjpm-", ".log")
        val startedAt = System.currentTimeMillis()
        val builder = ProcessBuilder(command)
            .directory(projectDir.toFile())
            .redirectErrorStream(true)
            .redirectOutput(outputFile.toFile())
        builder.environment().apply {
            put("CANGJIE_HOME", config.cangjieHome.toString())
            // cjpm 依赖 SDK 的运行时 DLL；只加 bin 会在 Windows 上报 STATUS_DLL_NOT_FOUND。
            put(
                "PATH",
                buildSdkPath(config) + java.io.File.pathSeparator + get("PATH").orEmpty(),
            )
        }

        val process = builder.start()
        val finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES)
        if (!finished) {
            process.destroyForcibly()
            error("cjpm 超时（${timeoutMinutes}min）: ${command.joinToString(" ")}")
        }
        val output = Files.readString(outputFile)
        Files.deleteIfExists(outputFile)

        return Result(
            command = command.joinToString(" "),
            exitCode = process.exitValue(),
            output = output,
            durationMillis = System.currentTimeMillis() - startedAt,
        )
    }
}
