package org.cangnova.cangjie.analysis.decompiled

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.analysis.decompiled.psi.CaStandaloneBinaryTextRenderer
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

/**
 * 锁定 `.cjo -> decompiled text` 的基础链路。
 *
 * 这里直接复用仓内 stdlib fixture，确保：
 * 1. `CjoManager` 能基于 `.cjo` 根正确枚举 package；
 * 2. standalone decompiler 能在无 project 容器时直接输出反编译文本。
 */
class CaDecompiledStandaloneRendererTest {
    @Test
    fun renderStdCorePackage() {
        withEnvironment {
            val stdlibRoot = locateStdlibFixtureRoot()
            val repository = CjoManager(
                CjoSearchPath { key ->
                    when (key) {
                        "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> stdlibRoot.toString()
                        else -> null
                    }
                },
            )

            val availablePackages = repository.getAvailablePackageNames()
            assertTrue(
                availablePackages.contains(FqName("std.core")),
                "stdlib fixture should expose `std.core`; actual=$availablePackages",
            )

            val renderedText = CaStandaloneBinaryTextRenderer.render(
                TestBinaryVirtualFile(
                    parentDirectoryPath = stdlibRoot.resolve("std").toString(),
                    fileName = "std.core.cjo",
                    binaryContent = stdlibRoot.resolve("std").resolve("std.core.cjo").readBytes(),
                ),
            )
            assertNotNull(renderedText, "standalone renderer should return decompiled text for `std.core`")
            assertTrue(
                renderedText!!.contains("package std.core"),
                "decompiled text should contain the real package declaration; actual=${renderedText.take(160)}",
            )
            assertTrue(renderedText.lineSequence().count() > 2, "decompiled text should not collapse to an empty file")
        }
    }

    @Test
    fun standaloneRendererReadsStdCoreBinaryFile() {
        withEnvironment {
            val stdCoreBinary = locateStdlibFixtureRoot().resolve("std").resolve("std.core.cjo")
            val virtualFile = TestBinaryVirtualFile(
                parentDirectoryPath = stdCoreBinary.parent.toString(),
                fileName = stdCoreBinary.fileName.toString(),
                binaryContent = stdCoreBinary.readBytes(),
            )

            val renderedText = CaStandaloneBinaryTextRenderer.render(virtualFile)
            assertNotNull(renderedText, "standalone renderer should support `.cjo` files directly")
            assertTrue(
                renderedText!!.contains("package std.core"),
                "standalone renderer output should preserve the package name; actual=${renderedText.take(160)}",
            )
        }
    }

    @Test
    fun standaloneRendererDerivesRepositoryRootForNestedStdPackage() {
        withEnvironment {
            val nestedBinary = locateStdlibFixtureRoot().resolve("std").resolve("std.database.sql.cjo")
            val virtualFile = TestBinaryVirtualFile(
                parentDirectoryPath = nestedBinary.parent.toString(),
                fileName = nestedBinary.fileName.toString(),
                binaryContent = nestedBinary.readBytes(),
            )

            val renderedText = CaStandaloneBinaryTextRenderer.render(virtualFile)
            assertNotNull(renderedText, "standalone renderer should handle nested stdlib packages")
            assertTrue(
                renderedText!!.contains("package std.database.sql"),
                "nested package output should preserve the full package name; actual=${renderedText.take(160)}",
            )
        }
    }

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

    private fun locateRepositoryRoot(start: Path): Path {
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    /**
     * `BinaryFileDecompiler` 场景只依赖二进制内容与父目录路径，
     * 这里用轻量内存文件锁定这个契约，避免把测试稳定性绑到本地文件系统实现上。
     */
    private class TestBinaryVirtualFile(
        parentDirectoryPath: String,
        fileName: String,
        private val binaryContent: ByteArray,
    ) : LightVirtualFile(fileName, CangJieBuiltInFileType, "") {
        private val parentDirectory = TestDirectoryVirtualFile(parentDirectoryPath)

        override fun getParent(): VirtualFile = parentDirectory

        override fun getPath(): String = "${parentDirectory.path}/$name"

        override fun contentsToByteArray(): ByteArray = binaryContent

        override fun getInputStream() = ByteArrayInputStream(binaryContent)
    }

    private class TestDirectoryVirtualFile(
        private val directoryPath: String,
    ) : LightVirtualFile(directoryPath.substringAfterLast('/', directoryPath.substringAfterLast('\\')), CangJieBuiltInFileType, "") {
        override fun isDirectory(): Boolean = true

        override fun getPath(): String = directoryPath
    }

    private fun withEnvironment(action: () -> Unit) {
        val disposable = Disposer.newDisposable("CaDecompiledStandaloneRendererTest")
        try {
            CangJieCoreEnvironment.createForTests(disposable)
            action()
        } finally {
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction {
                    Disposer.dispose(disposable)
                }
            } else {
                Disposer.dispose(disposable)
            }
        }
    }
}
