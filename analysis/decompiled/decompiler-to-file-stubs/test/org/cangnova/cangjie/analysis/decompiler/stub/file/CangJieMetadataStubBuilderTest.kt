package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

/**
 * 锁定 `.cjo` metadata stub builder 的项目无关契约。
 *
 * `hasMetadata` / `hasStub` 只做头部读，不依赖 project 与服务容器；`readFile` 需要 project，
 * 无 project 时必须返回 null（detached owner 由带 project 的 `DecompiledCjoModuleDataProvider` 提供，
 * 不是 stub builder 的职责）。
 */
class CangJieMetadataStubBuilderTest {
    /**
     * 标准库 `std.core.cjo` 的 metadata 在无 project 环境下可读，`hasStub` 与 `isSupported` 一致。
     */
    @Test
    fun hasMetadataIsReadableWithoutProject() {
        val builder = HeaderOnlyStubBuilder()
        val virtualFile = cjoVirtualFile(locateStdlibFixtureRoot().resolve("std").resolve("std.core.cjo"))

        assertTrue(builder.hasMetadataOf(virtualFile))
        assertTrue(builder.isSupported(virtualFile))
        assertTrue(builder.hasStub(virtualFile))
    }

    /**
     * 无 project 时 `readFile` 返回 null：owner 解析需要项目结构，不能在无 project 下凭空构造。
     */
    @Test
    fun readFileWithoutProjectReturnsNull() {
        val builder = HeaderOnlyStubBuilder()
        val virtualFile = cjoVirtualFile(locateStdlibFixtureRoot().resolve("std").resolve("std.core.cjo"))

        assertNull(builder.readFileSafely(virtualFile))
        assertNull(builder.readFileWithoutProject(virtualFile))
    }

    /**
     * 非 `.cjo` 文件不进入 stub 构建；扩展名判定不触碰内容。
     */
    @Test
    fun unsupportedFileIsRejected() {
        val builder = HeaderOnlyStubBuilder()
        val textFile = LightVirtualFile("main.cj")

        assertFalse(builder.isSupported(textFile))
        assertFalse(builder.hasStub(textFile))
    }

    /**
     * VFS 瞬时失效的文件（`!isValid`）既不进入 stub 构建，也不会触发头部读。
     *
     * `readSafely` 在调用动作前检查 `isValid`，所以读取层看不到这个文件；这正是启动期打开
     * 标签页时 `.cjo` 可能处于的状态，不能被当成“头部读失败”记日志。
     */
    @Test
    fun invalidFileIsRejectedWithoutHeaderRead() {
        val builder = HeaderOnlyStubBuilder()
        val content = locateStdlibFixtureRoot().resolve("std").resolve("std.core.cjo").readBytes()
        var headerReads = 0
        val invalid = object : LightVirtualFile("std.core.cjo", CangJieBuiltInFileType, "") {
            private val bytes: ByteArray = content

            override fun isValid(): Boolean = false

            override fun contentsToByteArray(): ByteArray {
                headerReads++
                return bytes
            }
        }

        assertFalse(builder.isSupported(invalid) && builder.hasStub(invalid))
        assertFalse(builder.hasStub(invalid))
        assertNull(builder.readFileSafely(invalid))
        assertEquals(0, headerReads, "invalid file must not reach the header read")
    }

    /**
     * 头部读在内容不是合法 `.cjo` 时返回 null 而不是抛异常。
     */
    @Test
    fun brokenBinaryHeaderIsRejectedWithoutException() {
        val builder = HeaderOnlyStubBuilder()
        val broken = object : LightVirtualFile("broken.cjo", CangJieBuiltInFileType, "") {
            private val bytes: ByteArray = ByteArray(64)

            override fun contentsToByteArray(): ByteArray = bytes

            override fun getInputStream(): java.io.InputStream = ByteArrayInputStream(bytes)
        }

        assertNull(CjoBinaryFileReader.readPackageFqName(broken))
        assertFalse(builder.hasStub(broken))
    }

    /**
     * 构造承载真实 `.cjo` 字节的轻量虚拟文件。
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

    /**
     * 与 `CangJieBuiltInMetadataStubBuilder` 形状相同的最小 stub builder：metadata 只做头部读。
     */
    private class HeaderOnlyStubBuilder : CangJieMetadataStubBuilder() {
        override val supportedFileType: FileType
            get() = CangJieBuiltInFileType

        override fun getStubVersion(): Int = 1

        override fun hasMetadata(virtualFile: VirtualFile): Boolean =
            CjoBinaryFileReader.readPackageFqName(virtualFile) != null

        /** 暴露受保护的 `hasMetadata` 供测试断言。 */
        fun hasMetadataOf(virtualFile: VirtualFile): Boolean = hasMetadata(virtualFile)

        /**
         * owner 解析需要 project；这里只暴露“无 project”分支供测试断言。
         */
        override fun readFile(virtualFile: VirtualFile, content: ByteArray?, project: Project?): FileWithMetadata? {
            project ?: return null
            return null
        }

        /** 以无 project 方式调用受保护的 `readFile`。 */
        fun readFileWithoutProject(virtualFile: VirtualFile): FileWithMetadata? = readFile(virtualFile, null, null)
    }
}
