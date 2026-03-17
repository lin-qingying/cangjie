package org.cangnova.cangjie.llvm.jni

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.io.path.createFile
import kotlin.io.path.writeText

/**
 * 原生库加载器测试。
 */
class NativeLibraryLoaderTest {
    @Test
    fun `loads from system property path with highest priority`() {
        val calls = mutableListOf<String>()
        val nativeFile = Files.createTempDirectory("loader-test").resolve("libcangjie_llvm_jni.so")
        nativeFile.createFile()
        withSystemProperty("cangjie.llvm.native.library.path", nativeFile.toString()) {
            val loader = NativeLibraryLoader(
                loadAbsolute = {
                    calls += "abs:$it"
                },
                loadByName = {
                    calls += "name:$it"
                },
                resourceOpener = { null },
                tempDirProvider = { Files.createTempDirectory("loader-test") },
            )
            val result = loader.load()
            assertTrue(result.loaded, result.diagnostics)
            assertNotNull(calls.firstOrNull())
        }
    }

    @Test
    fun `loads from classpath resource when property is missing`() {
        val calls = mutableListOf<String>()
        withSystemProperty("cangjie.llvm.native.library.path", null) {
            val loader = NativeLibraryLoader(
                loadAbsolute = { calls += "abs:$it" },
                loadByName = { calls += "name:$it" },
                resourceOpener = { ByteArrayInputStream(byteArrayOf(1, 2, 3)) },
                tempDirProvider = { Files.createTempDirectory("loader-test") },
            )
            val result = loader.load()
            assertTrue(result.loaded, result.diagnostics)
            assertTrue(calls.any { it.startsWith("abs:") })
            assertFalse(calls.any { it.startsWith("name:") })
        }
    }

    @Test
    fun `loads from native home and preloads deps order`() {
        val calls = mutableListOf<String>()
        withSystemProperty("cangjie.llvm.native.library.path", null) {
            val home = Files.createTempDirectory("loader-home")
            val platform = PlatformDetector.detect()
            val platformDir = home.resolve("native").resolve(platform).also { Files.createDirectories(it) }
            val mainFile = libraryFileName(platform)
            platformDir.resolve(mainFile).createFile()
            platformDir.resolve("dep-b.dll").createFile()
            platformDir.resolve("dep-a.dll").createFile()
            platformDir.resolve("deps.order").writeText("dep-b.dll\ndep-a.dll\n")

            withSystemProperty("cangjie.native.home", home.toString()) {
                val loader = NativeLibraryLoader(
                    loadAbsolute = { calls += it },
                    loadByName = { calls += "name:$it" },
                    resourceOpener = { null },
                    tempDirProvider = { Files.createTempDirectory("loader-test") },
                )
                val result = loader.load()
                assertTrue(result.loaded, result.diagnostics)
                assertTrue(calls[0].endsWith("dep-b.dll"))
                assertTrue(calls[1].endsWith("dep-a.dll"))
                assertTrue(calls.last().endsWith(mainFile))
            }
        }
    }

    @Test
    fun `returns diagnostics when all loading strategies fail`() {
        withSystemProperty("cangjie.llvm.native.library.path", null) {
            val loader = NativeLibraryLoader(
                loadAbsolute = { throw UnsatisfiedLinkError("abs failed") },
                loadByName = { throw UnsatisfiedLinkError("name failed") },
                resourceOpener = { null },
                tempDirProvider = { Files.createTempDirectory("loader-test") },
            )
            val result = loader.load()
            assertFalse(result.loaded)
            assertTrue(result.diagnostics.contains("classpath resource not found"))
            assertTrue(result.diagnostics.contains("system library load failed"))
        }
    }

    private fun libraryFileName(platformId: String): String {
        return when {
            platformId.startsWith("windows-") -> "cangjie_llvm_jni.dll"
            platformId.startsWith("macos-") -> "libcangjie_llvm_jni.dylib"
            else -> "libcangjie_llvm_jni.so"
        }
    }
}

/**
 * 临时覆盖系统属性的测试辅助函数。
 */
private inline fun withSystemProperty(key: String, value: String?, block: () -> Unit) {
    val previous = System.getProperty(key)
    if (value == null) {
        System.clearProperty(key)
    } else {
        System.setProperty(key, value)
    }
    try {
        block()
    } finally {
        if (previous == null) {
            System.clearProperty(key)
        } else {
            System.setProperty(key, previous)
        }
    }
}
