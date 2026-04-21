package org.cangnova.cangjie.cfir.analysis.tests.golden

import java.io.File
import java.nio.file.Path

/**
 * 调用 cjc 官方编译器进程，管理临时文件。
 */
object CjcProcessRunner {

    /**
     * 发现 cjc 可执行文件路径。
     *
     * 搜索顺序：
     * 1. 环境变量 CANGJIE_HOME
     * 2. System property cjc.home
     * 3. 用户 home/sdk 下最新的 cangjie-sdk-* 目录
     */
    fun findCjcPath(): Path {
        System.getenv("CANGJIE_HOME")?.let { home ->
            val cjc = Path.of(home, "bin", "cjc.exe")
            if (cjc.toFile().exists()) return cjc
        }
        System.getProperty("cjc.home")?.let { home ->
            val cjc = Path.of(home, "bin", "cjc.exe")
            if (cjc.toFile().exists()) return cjc
        }
        val userHome = Path.of(System.getProperty("user.home"))
        userHome.resolve("sdk").toFile().listFiles()
            ?.filter { it.name.startsWith("cangjie-sdk-") }
            ?.sortedByDescending { it.name }
            ?.firstOrNull()
            ?.let {
                val cjc = it.toPath().resolve("cangjie/bin/cjc.exe")
                if (cjc.toFile().exists()) return cjc
            }
        error("Cannot find cjc. Set CANGJIE_HOME environment variable.")
    }

    /**
     * 编译单个 .cj 文件，返回 JSON 诊断输出。
     *
     * @param cjcPath cjc 可执行文件路径
     * @param sourceFile 要编译的 .cj 文件
     * @param noPrelude 是否显式关闭 prelude
     * @return JSON 诊断字符串（可能为空表示无输出）
     */
    fun compileSingleFile(
        cjcPath: Path,
        sourceFile: File,
        noPrelude: Boolean = false,
    ): CjcCompilationResult {
        val args = mutableListOf(
            cjcPath.toString(),
            sourceFile.absolutePath,
            "--diagnostic-format", "json",
        )
        if (noPrelude) {
            args += "--no-prelude"
        }

        val process = ProcessBuilder(args).apply {
            redirectErrorStream(true)
        }.start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return CjcCompilationResult(output, exitCode)
    }

    /**
     * 编译包目录，生成 staticlib 输出。
     *
     * @param cjcPath cjc 可执行文件路径
     * @param packageDir 包目录
     * @param outputFile 输出文件
     * @param importPaths 导入路径列表
     * @return JSON 诊断字符串
     */
    fun compilePackage(
        cjcPath: Path,
        packageDir: File,
        outputFile: File,
        importPaths: List<File> = emptyList(),
    ): CjcCompilationResult {
        val args = mutableListOf(
            cjcPath.toString(),
            "-p", packageDir.absolutePath,
            "--output-type=staticlib",
            "--no-sub-pkg",
            "-o", outputFile.absolutePath,
            "--diagnostic-format", "json",
        )
        for (path in importPaths) {
            args += "--import-path"
            args += path.absolutePath
        }

        val process = ProcessBuilder(args).apply {
            redirectErrorStream(true)
        }.start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return CjcCompilationResult(output, exitCode)
    }

    data class CjcCompilationResult(
        val output: String,
        val exitCode: Int,
    )
}
