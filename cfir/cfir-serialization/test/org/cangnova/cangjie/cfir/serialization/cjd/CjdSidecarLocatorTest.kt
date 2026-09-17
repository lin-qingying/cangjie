package org.cangnova.cangjie.cfir.serialization.cjd

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** 验证 sidecar 的精确路径契约及缺失文件的静默回退。 */
class CjdSidecarLocatorTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `derive sibling declaration path`() {
        assertEquals(directory.resolve("std.core.cj.d"), CjdSidecarLocator.deriveCjdPath(directory.resolve("std.core.cjo")))
    }

    @Test
    fun `preserve directory and earlier cjo segments`() {
        val parent = directory.resolve("sdk.cjo.dir")
        assertEquals(parent.resolve("lib.cjo.cj.d"), CjdSidecarLocator.deriveCjdPath(parent.resolve("lib.cjo.cjo")))
    }

    @Test
    fun `preserve relative path`() {
        assertEquals(Path.of("lib.cj.d"), CjdSidecarLocator.deriveCjdPath(Path.of("lib.cjo")))
    }

    @Test
    fun `reject non cjo suffixes`() {
        for (name in listOf("lib", "lib.CJO", "lib.cjo.txt", "lib.cj.d")) {
            assertFailsWith<IllegalArgumentException>(name) {
                CjdSidecarLocator.deriveCjdPath(directory.resolve(name))
            }
        }
    }

    @Test
    fun `reject filesystem root`() {
        assertFailsWith<IllegalArgumentException> {
            CjdSidecarLocator.deriveCjdPath(directory.toAbsolutePath().root)
        }
    }

    @Test
    fun `missing sidecar is ignored`() {
        assertNull(CjdSidecarLocator.findReadable(directory.resolve("lib.cjo")))
    }

    @Test
    fun `directory is not a sidecar`() {
        Files.createDirectory(directory.resolve("lib.cj.d"))
        assertNull(CjdSidecarLocator.findReadable(directory.resolve("lib.cjo")))
    }

    @Test
    fun `readable sidecar is found without reading its contents`() {
        val sidecar = Files.writeString(directory.resolve("lib.cj.d"), "package lib\npublic func api(): Unit\n")
        assertEquals(sidecar, CjdSidecarLocator.findReadable(directory.resolve("lib.cjo")))
    }

    @Test
    fun `missing result is not cached`() {
        val cjoPath = directory.resolve("lib.cjo")
        assertNull(CjdSidecarLocator.findReadable(cjoPath))
        val sidecar = Files.createFile(directory.resolve("lib.cj.d"))
        assertEquals(sidecar, CjdSidecarLocator.findReadable(cjoPath))
    }
}
