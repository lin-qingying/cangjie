package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.CjoConstants
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CjoSearchPathTest {
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
}
