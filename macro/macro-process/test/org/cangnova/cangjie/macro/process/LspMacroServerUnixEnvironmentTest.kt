package org.cangnova.cangjie.macro.process

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LspMacroServerUnixEnvironmentTest {
    @Test
    fun `dynamic library variable follows platform`() {
        assertEquals("LD_LIBRARY_PATH", unixDynamicLibraryPathVariable("Linux"))
        assertEquals("DYLD_LIBRARY_PATH", unixDynamicLibraryPathVariable("Mac OS X"))
    }

    @Test
    fun `directories include sdk runtime and lib platform dirs`(@TempDir sdk: File) {
        val runtime = File(sdk, "runtime/lib/linux_x86_64_cjnative").apply { mkdirs() }
        val lib = File(sdk, "lib/linux_x86_64_cjnative").apply { mkdirs() }

        val directories = unixDynamicLibraryDirectories(sdk.absolutePath)

        assertTrue(directories.contains(runtime.absolutePath))
        assertTrue(directories.contains(lib.absolutePath))
    }

    @Test
    fun `missing sdk dirs produce empty list`(@TempDir sdk: File) {
        assertTrue(unixDynamicLibraryDirectories(sdk.absolutePath).isEmpty())
    }
}
