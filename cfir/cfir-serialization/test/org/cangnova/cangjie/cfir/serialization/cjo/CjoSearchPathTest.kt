package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.CjoConstants
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证 CJO 搜索路径对标准库目录和普通库目录的优先级规则。
 */
class CjoSearchPathTest {
    /** 实际 SDK 包头相同也不能替换显式模块根中的资源；文件名不参与包身份推断。 */
    @Test
    fun `explicit own std package precedes environment SDK header index`() {
        val directory = Files.createTempDirectory("cjo-own-stdlib-")
        try {
            val ownRoot = directory.resolve("own").createDirectories()
            val sdkRoot = directory.resolve("sdk").createDirectories()
            val bytes = checkNotNull(javaClass.classLoader.getResourceAsStream(
                "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo",
            )).use { it.readBytes() }
            val ownFile = Files.write(ownRoot.resolve("module-owned.cjo"), bytes)
            Files.write(sdkRoot.resolve("sdk-owned.cjo"), bytes)
            val search = CjoSearchPath(additionalLibrarySearchPaths = listOf(ownRoot.toFile()),
                envProvider = { key -> if (key == "CANGJIE_STDLIB_MODULE") sdkRoot.toString() else null })
            assertEquals(ownFile.toFile().absolutePath, search.findCjoFile("std.core")?.absolutePath)
            assertEquals(ownFile.toFile().absolutePath, search.findCjoFile("std.core")?.absolutePath)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    /** 当前显式文件的读取失败不能让后面的 SDK 同名包代替它。 */
    @Test
    fun `explicit own standard path precedes later readable SDK header`() {
        val directory = Files.createTempDirectory("cjo-own-standard-path-")
        try {
            val ownRoot = directory.resolve("own").createDirectories()
            val sdkRoot = directory.resolve("sdk").createDirectories()
            val ownFile = ownRoot.resolve(CjoConstants.packageNameToPath("std.core"))
            ownFile.parent.createDirectories()
            Files.write(ownFile, byteArrayOf(1))
            val bytes = checkNotNull(javaClass.classLoader.getResourceAsStream(
                "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo",
            )).use { it.readBytes() }
            Files.write(sdkRoot.resolve("sdk-owned.cjo"), bytes)
            val search = CjoSearchPath(additionalLibrarySearchPaths = listOf(ownRoot.toFile()),
                envProvider = { key -> if (key == "CANGJIE_STDLIB_MODULE") sdkRoot.toString() else null })
            assertEquals(ownFile.toFile().absolutePath, search.findCjoFile("std.core")?.absolutePath)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    /**
     * 验证非 std 包不能从 CANGJIE_STDLIB_MODULE 路径加载。
     */
    @Test
    fun `non std package must not be loaded from CANGJIE_STDLIB_MODULE`() {
        val stdlibDir = Files.createTempDirectory("cjo-stdlib-")
        val libraryDir = Files.createTempDirectory("cjo-lib-")

        try {
            val fullPkgName = "foo.bar"
            val cjoRelativePath = CjoConstants.packageNameToPath(fullPkgName)

            val stdlibCjo = stdlibDir.resolve(cjoRelativePath)
            stdlibCjo.parent?.createDirectories()
            stdlibCjo.outputStream().use { it.write(byteArrayOf(1, 2, 3)) }

            val searchPath = CjoSearchPath(
                envProvider = { key ->
                    when (key) {
                        "CANGJIE_STDLIB_MODULE" -> stdlibDir.toString()
                        "CANGJIE_LIBRARY" -> libraryDir.toString()
                        else -> null
                    }
                }
            )

            assertNull(searchPath.findCjoFile(fullPkgName))
        } finally {
            stdlibDir.toFile().deleteRecursively()
            libraryDir.toFile().deleteRecursively()
        }
    }

    /**
     * 验证 std 包优先从 CANGJIE_STDLIB_MODULE 路径加载。
     */
    @Test
    fun `std package loads from CANGJIE_STDLIB_MODULE with higher priority`() {
        val stdlibDir = Files.createTempDirectory("cjo-stdlib-")
        val libraryDir = Files.createTempDirectory("cjo-lib-")

        try {
            val fullPkgName = "std.core"
            val cjoRelativePath = CjoConstants.packageNameToPath(fullPkgName)

            val stdlibCjo = stdlibDir.resolve(cjoRelativePath)
            stdlibCjo.parent?.createDirectories()
            stdlibCjo.outputStream().use { it.write(byteArrayOf(1)) }

            val libraryCjo = libraryDir.resolve(cjoRelativePath)
            libraryCjo.parent?.createDirectories()
            libraryCjo.outputStream().use { it.write(byteArrayOf(2)) }

            val searchPath = CjoSearchPath(
                envProvider = { key ->
                    when (key) {
                        "CANGJIE_STDLIB_MODULE" -> stdlibDir.toString()
                        "CANGJIE_LIBRARY" -> libraryDir.toString()
                        else -> null
                    }
                }
            )

            val resolved = searchPath.findCjoFile(fullPkgName)
            assertEquals(stdlibCjo.toFile().absolutePath, resolved?.absolutePath)
        } finally {
            stdlibDir.toFile().deleteRecursively()
            libraryDir.toFile().deleteRecursively()
        }
    }

    /**
     * 验证 binary library 模块显式提供的根目录可参与普通 CJO 包搜索。
     */
    @Test
    fun `library package loads from explicit module search root`() {
        val libraryDir = Files.createTempDirectory("cjo-explicit-lib-")
        try {
            val fullPkgName = "vendor.api"
            val cjoFile = libraryDir.resolve(CjoConstants.packageNameToPath(fullPkgName))
            cjoFile.parent?.createDirectories()
            cjoFile.outputStream().use { it.write(byteArrayOf(1, 2, 3)) }

            val searchPath = CjoSearchPath(
                additionalLibrarySearchPaths = listOf(libraryDir.toFile()),
                envProvider = { null },
            )

            assertEquals(cjoFile.toFile().absolutePath, searchPath.findCjoFile(fullPkgName)?.absolutePath)
        } finally {
            libraryDir.toFile().deleteRecursively()
        }
    }
}
