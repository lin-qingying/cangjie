package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

/**
 * 锁定 `.cjo` 头部读取能力：
 * - 能从 binary file 直接恢复 package 名；
 * - 不需要 project/service 容器。
 */
class CjoBinaryFileReaderTest {
    /**
     * 验证标准库 `std.core.cjo` 的 package 名可以仅通过二进制头部读取恢复。
     */
    @Test
    fun readPackageFqNameFromStdCoreBinary() {
        val stdCoreBinary = locateStdlibFixtureRoot().resolve("std").resolve("std.core.cjo")
        val virtualFile = object : LightVirtualFile(stdCoreBinary.fileName.toString(), CangJieBuiltInFileType, "") {
            private val content = stdCoreBinary.readBytes()

            override fun contentsToByteArray(): ByteArray = content

            override fun getInputStream() = ByteArrayInputStream(content)
        }

        val packageFqName = CjoBinaryFileReader.readPackageFqName(virtualFile)
        assertNotNull(packageFqName, "reader should extract package fqName from `.cjo`")
        assertEquals(FqName("std.core"), packageFqName)
    }

    /**
     * 定位仓库中作为 `.cjo` 读取测试输入的标准库 fixture 根目录。
     */
    private fun locateStdlibFixtureRoot(): Path {
        val repoRoot = locateRepositoryRoot(Paths.get("").toAbsolutePath().normalize())
        val fixtureRoot = repoRoot
            .resolve("cfir")
            .resolve("cfir-serialization")
            .resolve("testResources")
            .resolve("cjo-sdk")
            .resolve("windows_x86_64_cjnative")

        require(fixtureRoot.resolve("std.cjo").isRegularFile()) {
            "Cannot locate stdlib fixture root under $fixtureRoot"
        }
        return fixtureRoot
    }

    /**
     * 从当前工作目录向上查找包含 `settings.gradle.kts` 的仓库根目录。
     */
    private fun locateRepositoryRoot(start: Path): Path {
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }
}
