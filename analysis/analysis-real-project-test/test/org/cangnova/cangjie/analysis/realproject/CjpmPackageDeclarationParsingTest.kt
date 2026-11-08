package org.cangnova.cangjie.analysis.realproject

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * [CjpmProjectModel] 对源码包声明的解析测试。
 *
 * [CjpmProjectModel.sourcePackages] 是「过滤项目自身产出 `.cjo`」的唯一依据：漏掉任何一个包，
 * 该包的 `.cjo` 就会被当作外部依赖挂载，同一份源码出现源码符号 + 库符号两份，
 * 表现为 `CLASSIFIER_REDECLARATION` / `AMBIGUOUS_USE` 等成片误报。
 * 仓颉允许包声明带访问修饰符（`protected package` 等），真实项目普遍使用，必须被识别。
 */
class CjpmPackageDeclarationParsingTest {

    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `access modifiers on package declaration are recognised`() {
        writeSource("connection/connection.cj", "protected package mariadb.connection")
        writeSource("protocol/command.cj", "internal package mariadb.protocol")
        writeSource("cdbc/column.cj", "public package mariadb.cdbc")
        writeSource("secret/secret.cj", "private package mariadb.secret")
        writeSource("root/root.cj", "package mariadb")

        assertEquals(
            setOf("mariadb", "mariadb.connection", "mariadb.protocol", "mariadb.cdbc", "mariadb.secret"),
            CjpmProjectModel.read(projectDir).sourcePackages,
        )
    }

    @Test
    fun `modifier separated by tabs and extra spaces is accepted`() {
        writeSource("a/a.cj", "\tprotected \t package \t mariadb.a")

        assertEquals(setOf("mariadb.a"), CjpmProjectModel.read(projectDir).sourcePackages)
    }

    @Test
    fun `indented declaration without modifier is accepted`() {
        writeSource("a/a.cj", "    package mariadb.a")

        assertEquals(setOf("mariadb.a"), CjpmProjectModel.read(projectDir).sourcePackages)
    }

    @Test
    fun `commented package declaration is ignored`() {
        writeSource("a/a.cj", "// package mariadb.commented\npackage mariadb.real")
        writeSource("b/b.cj", " * package mariadb.doc")

        assertEquals(setOf("mariadb.real"), CjpmProjectModel.read(projectDir).sourcePackages)
    }

    /** 包名只取标识符序列，行尾注释不进入包名。 */
    @Test
    fun `trailing comment does not leak into package name`() {
        writeSource("a/a.cj", "protected package mariadb.a // 连接层")

        assertEquals(setOf("mariadb.a"), CjpmProjectModel.read(projectDir).sourcePackages)
    }

    private fun writeSource(relativePath: String, content: String) {
        val file = projectDir.resolve("src").createDirectories().resolve(relativePath)
        Files.createDirectories(file.parent)
        file.writeText(content)
    }
}
