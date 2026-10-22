package org.cangnova.cangjie.analysis.realproject

import java.nio.file.Files
import java.nio.file.Path

/**
 * 单个真实项目的运行配置。
 *
 * @property id 项目标识，用于报告与过滤。
 * @property repo git 地址（支持本地路径）。
 * @property ref 固定提交（SHA 或分支）。
 * @property cangjieHome 仓颉 SDK 根目录，`bin` 或 `tools/bin` 下的 cjpm 从这里解析。
 * @property workspaceRoot 检出与报告根目录。
 * @property refresh 已有检出时是否重新 fetch；false 表示离线复用。
 */
data class RealProjectConfig(
    val id: String,
    val repo: String,
    val ref: String,
    val cangjieHome: Path,
    val workspaceRoot: Path,
    val refresh: Boolean = true,
) {
    /** 当前配置对应的项目检出目录；目录名剔除 Windows 非法字符与结尾的点。 */
    val checkoutDir: Path
        get() = workspaceRoot.resolve("checkout").resolve("${sanitize(id)}-${sanitize(ref.take(12))}")

    /** 保证目录名在 Windows 上合法（ref 可能以 `.` 结尾）。 */
    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trimEnd('.', ' ')
            .ifBlank { "ref" }

    /** 与 SDK 匹配的 cjpm 可执行文件；兼容 `bin/` 与 `tools/bin/` 两种 SDK 布局。 */
    val cjpmExecutable: Path
        get() {
            val executableName = if (isWindows) "cjpm.exe" else "cjpm"
            val candidates = listOf(
                cangjieHome.resolve("bin").resolve(executableName),
                cangjieHome.resolve("tools").resolve("bin").resolve(executableName),
            )
            return candidates.firstOrNull(Files::isExecutable) ?: candidates.first()
        }

    private companion object {
        private val isWindows: Boolean
            get() = System.getProperty("os.name").orEmpty().lowercase().contains("win")
    }
}
