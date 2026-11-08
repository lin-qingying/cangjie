package org.cangnova.cangjie.analysis.realproject

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 真实项目的 cjpm 结构：包名、源码根、源文件与 `.cjo` 产物。
 *
 * @property name `cjpm.toml` 的包名，缺省用目录名。
 * @property sourceRoots 参与分析的源码根。
 * @property cjFiles 源码根下的全部 `.cj` 文件。
 * @property sourcePackages 源码中声明的包名集合，用于过滤项目自身产出的 `.cjo`。
 * @property cjoFiles `target/` 下的全部 `.cjo` 文件。
 * @property binDependencyRoots `cjpm.toml` 中 `[target.<triple>.bin-dependencies]` 的
 *   `path-option` 目录（已展开 `${ENV}` 占位符并按当前平台过滤）。cjpm 不会把这类
 *   预编译库（如 stdx）的 `.cjo` 拷进 `target/`，必须单独挂载才能参与解析。
 */
data class CjpmProjectModel(
    val name: String,
    val sourceRoots: List<Path>,
    val cjFiles: List<Path>,
    val sourcePackages: Set<String>,
    val cjoFiles: List<Path>,
    val binDependencyRoots: List<Path> = emptyList(),
) {
    companion object {
        /**
         * 包声明：可带访问修饰符前缀。
         *
         * 仓颉允许 `public|protected|internal|private package foo.bar`，真实项目（如 mariadb-driver）
         * 大量使用 `protected package`。不识别修饰符会让这些包名漏出 [sourcePackages]，
         * 项目自身产出的 `.cjo` 于是被误当外部依赖挂载，与源码形成双份同名符号。
         */
        private val PACKAGE_DECLARATION = Regex(
            """(?m)^[ \t]*(?:(?:public|protected|internal|private)[ \t]+)?package[ \t]+([A-Za-z_][A-Za-z0-9_.]*)""",
        )

        /** `[target.<triple>.bin-dependencies]` 段名解析；triple 只允许 cjpm 使用的字母数字与连字符。 */
        private val BIN_DEPENDENCIES_SECTION = Regex("""^target\.([A-Za-z0-9_-]+)\.bin-dependencies$""")

        /** `${ENV_VAR}` 占位符。 */
        private val ENV_PLACEHOLDER = Regex("""\$\{([A-Za-z0-9_]+)\}""")

        /** 读取项目结构；`cjpm build` 之后调用才能看到完整的 `.cjo`。 */
        fun read(
            projectDir: Path,
            envProvider: (String) -> String? = System::getenv,
        ): CjpmProjectModel {
            val toml = projectDir.resolve("cjpm.toml")
            val tomlLines = if (Files.isRegularFile(toml)) Files.readAllLines(toml) else emptyList()
            val (name, srcDir) = readPackageSection(tomlLines)
            val sourceRoot = projectDir.resolve(srcDir).takeIf(Files::isDirectory) ?: projectDir
            val cjFiles = walk(sourceRoot) { it.toString().endsWith(".cj") }
            val cjoFiles = walk(projectDir.resolve("target")) { it.toString().endsWith(".cjo") }
            val sourcePackages = cjFiles.mapNotNullTo(linkedSetOf()) { file ->
                PACKAGE_DECLARATION.find(Files.readString(file))?.groupValues?.get(1)
            }
            return CjpmProjectModel(
                name = name.ifBlank { projectDir.fileName.toString() },
                sourceRoots = listOf(sourceRoot),
                cjFiles = cjFiles,
                sourcePackages = sourcePackages,
                cjoFiles = cjoFiles,
                binDependencyRoots = readBinDependencyRoots(tomlLines, envProvider),
            )
        }

        /**
         * 解析 `[target.<triple>.bin-dependencies]` 的 `path-option` 目录。
         *
         * 与 `readPackageSection` 同一套逐行 section 状态机（不引 TOML 库）：
         * 只接受当前平台 triple 命中的段；条目展开 `${ENV}` 占位符后过滤不存在的目录。
         * 无法解析的占位符（环境变量未设置）按整条丢弃，与 cjpm 把该路径传给 cjc 前的行为一致。
         */
        private fun readBinDependencyRoots(
            tomlLines: List<String>,
            envProvider: (String) -> String?,
        ): List<Path> {
            val currentTriples = currentTargetTriples
            if (currentTriples.isEmpty()) return emptyList()

            var section = ""
            val rawRoots = linkedSetOf<String>()
            tomlLines.forEach { rawLine ->
                val line = rawLine.substringBefore('#').trim()
                if (line.startsWith('[') && line.endsWith(']')) {
                    section = line.trim('[', ']').trim()
                    return@forEach
                }
                val sectionMatch = BIN_DEPENDENCIES_SECTION.matchEntire(section) ?: return@forEach
                if (sectionMatch.groupValues[1] !in currentTriples) return@forEach
                if (!line.startsWith("path-option")) return@forEach

                val value = line.substringAfter('=', "").trim()
                value.removePrefix("[").removeSuffix("]")
                    .split(',')
                    .map { entry -> entry.trim().removeSurrounding("\"").trim() }
                    .filter { entry -> entry.isNotEmpty() }
                    .forEach { entry ->
                        expandEnvPlaceholders(entry, envProvider)?.let { rawRoots += it }
                    }
            }
            return rawRoots.map { root -> Paths.get(root) }
                .filter(Files::isDirectory)
                .toList()
        }

        /** 展开 `${ENV}` 占位符；任一占位符无法解析时返回 `null` 表示整条丢弃。 */
        private fun expandEnvPlaceholders(
            value: String,
            envProvider: (String) -> String?,
        ): String? {
            var unresolvable = false
            val expanded = ENV_PLACEHOLDER.replace(value) { match ->
                envProvider(match.groupValues[1]).also { resolved ->
                    if (resolved == null) unresolvable = true
                }.orEmpty()
            }
            return expanded.takeIf { it.isNotBlank() && !unresolvable }
        }

        /**
         * 当前 OS + 架构对应的 cjpm target triple 集合。
         *
         * 与 `CjpmRunner` 运行 cjpm 的机器是同一台，因此按 JVM 的 `os.name` / `os.arch` 推导。
         */
        val currentTargetTriples: Set<String>
            get() {
                val os = System.getProperty("os.name").orEmpty().lowercase()
                val arch = System.getProperty("os.arch").orEmpty().lowercase()
                val archPrefix = if (arch.contains("aarch64") || arch.contains("arm64")) "aarch64" else "x86_64"
                return when {
                    os.contains("win") -> setOf("$archPrefix-w64-mingw32")
                    os.contains("mac") || os.contains("darwin") -> setOf("$archPrefix-apple-darwin")
                    os.contains("linux") -> setOf("$archPrefix-unknown-linux-gnu")
                    else -> emptySet()
                }
            }

        private fun readPackageSection(tomlLines: List<String>): Pair<String, String> {
            var name = ""
            var srcDir = "src"
            var section = ""
            tomlLines.forEach { rawLine ->
                val line = rawLine.substringBefore('#').trim()
                if (line.startsWith('[') && line.endsWith(']')) {
                    section = line.trim('[', ']')
                    return@forEach
                }
                if (section != "package") return@forEach
                val key = line.substringBefore('=', "").trim()
                val value = line.substringAfter('=', "").trim().removeSurrounding("\"")
                when (key) {
                    "name" -> if (value.isNotEmpty()) name = value
                    "src-dir" -> if (value.isNotEmpty()) srcDir = value
                }
            }
            return name to srcDir
        }

        private fun walk(root: Path, predicate: (Path) -> Boolean): List<Path> {
            if (!Files.exists(root)) return emptyList()
            return Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && predicate(it) }.toList()
            }
        }
    }
}
