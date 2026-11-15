package org.cangnova.cangjie.analysis.realproject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

/**
 * [CjpmProjectModel] 解析 `[target.<triple>.bin-dependencies]` 的单元测试。
 *
 * 真实项目（mariadb-driver 等）通过 `path-option = ["${CANGJIE_STDX_PATH}"]` 引用
 * SDK 外置预编译库（stdx）；cjpm build 不会把这些 `.cjo` 拷进 `target/`，
 * 真实项目测试基建必须自己解析出目录根并挂载，否则 `import stdx.*` 无法解析。
 */
class CjpmBinDependenciesParsingTest {
    private val currentTargetTriple: String
        get() =
            CjpmProjectModel.currentTargetTriples.firstOrNull()
                ?: error("当前测试平台没有可识别的仓颉 target triple")

    @TempDir
    lateinit var projectDir: Path

    /** 环境变量注入桩：模拟 `${CANGJIE_STDX_PATH}` 等占位符解析。 */
    private fun envProvider(vararg entries: Pair<String, String>): (String) -> String? = entries.toMap()::get

    @Test
    fun `env placeholder expands to existing directory`() {
        val stdxRoot = Files.createDirectories(projectDir.resolve("stdx-root"))
        writeCjpmToml(
            """
            [target.$currentTargetTriple.bin-dependencies]
              path-option = [ "${'$'}{CANGJIE_STDX_PATH}" ]
            """.trimIndent(),
        )

        val model = CjpmProjectModel.read(projectDir, envProvider("CANGJIE_STDX_PATH" to stdxRoot.toString()))

        assertEquals(listOf(stdxRoot), model.binDependencyRoots)
    }

    @Test
    fun `unresolvable env placeholder drops the entry`() {
        writeCjpmToml(
            """
            [target.$currentTargetTriple.bin-dependencies]
              path-option = [ "${'$'}{CANGJIE_STDX_PATH}" ]
            """.trimIndent(),
        )

        val model = CjpmProjectModel.read(projectDir, envProvider())

        assertTrue(model.binDependencyRoots.isEmpty())
    }

    @Test
    fun `nonexistent directory is filtered out`() {
        writeCjpmToml(
            """
            [target.$currentTargetTriple.bin-dependencies]
              path-option = [ "${'$'}{CANGJIE_STDX_PATH}" ]
            """.trimIndent(),
        )

        // 环境变量指向不存在的目录：解析成功但被存在性过滤。
        val model =
            CjpmProjectModel.read(
                projectDir,
                envProvider("CANGJIE_STDX_PATH" to projectDir.resolve("missing").toString()),
            )

        assertTrue(model.binDependencyRoots.isEmpty())
    }

    @Test
    fun `other target triples are ignored`() {
        val stdxRoot = Files.createDirectories(projectDir.resolve("stdx-root"))
        val foreignArch = if (currentTargetTriple.startsWith("x86_64-")) "aarch64" else "x86_64"
        val foreignTriple = currentTargetTriple.replaceFirst(Regex("^(x86_64|aarch64)"), foreignArch)
        writeCjpmToml(
            """
            [target.$foreignTriple.bin-dependencies]
              path-option = [ "${'$'}{CANGJIE_STDX_PATH}" ]
            """.trimIndent(),
        )

        val model =
            CjpmProjectModel.read(
                projectDir,
                envProvider("CANGJIE_STDX_PATH" to stdxRoot.toString()),
            )

        assertTrue(model.binDependencyRoots.isEmpty())
    }

    @Test
    fun `plain path without placeholder is accepted`() {
        val stdxRoot = Files.createDirectories(projectDir.resolve("stdx-root"))
        writeCjpmToml(
            """
            [target.$currentTargetTriple.bin-dependencies]
              path-option = [ "${stdxRoot.toAbsolutePath().normalize().toString().replace('\\', '/')}" ]
            """.trimIndent(),
        )

        val model = CjpmProjectModel.read(projectDir, envProvider())

        // 路径大小写在 Windows 上不影响目录判定。
        assertEquals(1, model.binDependencyRoots.size)
    }

    @Test
    fun `multiple entries and deduplication`() {
        val rootA = Files.createDirectories(projectDir.resolve("root-a"))
        val rootB = Files.createDirectories(projectDir.resolve("root-b"))
        writeCjpmToml(
            """
            [target.$currentTargetTriple.bin-dependencies]
              path-option = [ "${'$'}{STDX_A}", "${'$'}{STDX_B}", "${'$'}{STDX_A}" ]
            """.trimIndent(),
        )

        val model =
            CjpmProjectModel.read(
                projectDir,
                envProvider("STDX_A" to rootA.toString(), "STDX_B" to rootB.toString()),
            )

        assertEquals(listOf(rootA, rootB), model.binDependencyRoots)
    }

    @Test
    fun `mixed package and bin-dependencies sections`() {
        val stdxRoot = Files.createDirectories(projectDir.resolve("stdx-root"))
        projectDir.resolve("src").createDirectory()
        writeCjpmToml(
            """
            [package]
              name = "mariadb"
              src-dir = "src"
            [target.$currentTargetTriple]
              [target.$currentTargetTriple.bin-dependencies]
                path-option = [ "${'$'}{CANGJIE_STDX_PATH}" ]
            """.trimIndent(),
        )

        val model =
            CjpmProjectModel.read(
                projectDir,
                envProvider("CANGJIE_STDX_PATH" to stdxRoot.toString()),
            )

        assertEquals("mariadb", model.name)
        assertEquals(
            "src",
            model.sourceRoots
                .single()
                .fileName
                .toString(),
        )
        assertEquals(listOf(stdxRoot), model.binDependencyRoots)
    }

    @Test
    fun `missing cjpm toml yields no bin dependency roots`() {
        val model = CjpmProjectModel.read(projectDir, envProvider())

        assertTrue(model.binDependencyRoots.isEmpty())
    }

    private fun writeCjpmToml(content: String) {
        projectDir.resolve("cjpm.toml").writeText(content)
    }
}
