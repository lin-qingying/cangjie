package org.cangnova.cangjie.analysis.realproject

import java.nio.file.Files
import java.nio.file.Path

/**
 * 真实项目的 cjpm 结构：包名、源码根、源文件与 `.cjo` 产物。
 *
 * @property name `cjpm.toml` 的包名，缺省用目录名。
 * @property sourceRoots 参与分析的源码根。
 * @property cjFiles 源码根下的全部 `.cj` 文件。
 * @property sourcePackages 源码中声明的包名集合，用于过滤项目自身产出的 `.cjo`。
 * @property cjoFiles `target/` 下的全部 `.cjo` 文件。
 */
data class CjpmProjectModel(
    val name: String,
    val sourceRoots: List<Path>,
    val cjFiles: List<Path>,
    val sourcePackages: Set<String>,
    val cjoFiles: List<Path>,
) {
    companion object {
        private val PACKAGE_DECLARATION = Regex("""(?m)^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)""")

        /** 读取项目结构；`cjpm build` 之后调用才能看到完整的 `.cjo`。 */
        fun read(projectDir: Path): CjpmProjectModel {
            val (name, srcDir) = readPackageSection(projectDir.resolve("cjpm.toml"))
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
            )
        }

        private fun readPackageSection(toml: Path): Pair<String, String> {
            if (!Files.isRegularFile(toml)) return "" to "src"
            var name = ""
            var srcDir = "src"
            var section = ""
            Files.readAllLines(toml).forEach { rawLine ->
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
