package org.cangnova.cangjie.llvm.jni

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files

class NativeLibraryLoaderTest {
    @Test
    fun `loads from system property path with highest priority`() {
        val calls = mutableListOf<String>()
        withSystemProperty("cangjie.llvm.native.library.path", "/tmp/libcangjie_llvm_jni.so") {
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
            assertTrue(calls.first().startsWith("abs:/tmp/libcangjie_llvm_jni.so"))
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
}

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
