package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

/**
 * 锁定 `.cjo` decompiler 的 `accepts` 契约。
 *
 * `accepts` 只回答“这条反编译链是否处理这个文件类型”，不回答“内容能否读出包名”：
 * 后者由 view provider 创建时的唯一一次 `hasStub` 检查负责。本测试在没有 project、
 * 没有 SDK、没有服务容器的环境下验证这一点，因此不会因为宿主配置差异而改变结果。
 */
class CangJieMetadataDecompilerAcceptanceTest {
    /**
     * 标准库 `std.core.cjo` 这样的真实二进制在无 project 环境下也必须被接受。
     */
    @Test
    fun realCjoBinaryIsAcceptedWithoutProject() {
        val decompiler = CangJieBuiltInDecompiler()
        val stubBuilder = decompiler.getStubBuilder()
        val virtualFile = cjoVirtualFile(locateStdlibFixtureRoot().resolve("std").resolve("std.core.cjo"))

        assertEquals(stubBuilder.isSupported(virtualFile), decompiler.accepts(virtualFile))
        assertTrue(decompiler.accepts(virtualFile), "`.cjo` file type must be accepted without project or SDK")
    }

    /**
     * 非 `.cjo` 文件一律不接受，与内容能否读取无关。
     */
    @Test
    fun nonCjoFileIsRejected() {
        val decompiler = CangJieBuiltInDecompiler()
        val stubBuilder = decompiler.getStubBuilder()
        val textFile = LightVirtualFile("main.cj")

        assertFalse(stubBuilder.isSupported(textFile))
        assertEquals(stubBuilder.isSupported(textFile), decompiler.accepts(textFile))
    }

    /**
     * 构造承载真实 `.cjo` 字节的轻量虚拟文件，扩展名与 file type 都按仓颉内建二进制注册。
     */
    private fun cjoVirtualFile(binary: Path): LightVirtualFile {
        val content = binary.readBytes()
        return object : LightVirtualFile(binary.fileName.toString(), CangJieBuiltInFileType, "") {
            private val bytes: ByteArray = content

            override fun contentsToByteArray(): ByteArray = bytes

            override fun getInputStream(): java.io.InputStream = ByteArrayInputStream(bytes)
        }
    }

    /**
     * 定位仓库中作为 `.cjo` 读取测试输入的标准库 fixture 根目录。
     */
    private fun locateStdlibFixtureRoot(): Path {
        val repoRoot = generateSequence(Paths.get("").toAbsolutePath().normalize()) { it.parent }
            .firstOrNull { it.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root")
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
}
