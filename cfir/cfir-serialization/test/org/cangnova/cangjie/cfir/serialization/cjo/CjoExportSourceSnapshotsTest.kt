package org.cangnova.cangjie.cfir.serialization.cjo

import org.junit.jupiter.api.io.TempDir
import java.nio.ReadOnlyBufferException
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertNotSame

/** 真实管理器读取事务的身份与所有权；同名、同路径和相同字节都不能代替快照身份。 */
class CjoExportSourceSnapshotsTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `repeated providers may share one actual snapshot`() {
        write(tempDir.resolve("shared.cjo"), "first")
        val manager = manager()
        val first = assertNotNull(manager.loadPackageSnapshot(PACKAGE))
        val second = assertNotNull(manager.loadPackageSnapshot(PACKAGE))
        assertSame(first, second)
        val selected = CjoExportSourceSnapshots.uniqueLoadedPackages(listOf(first, second, first))
        assertEquals(1, selected.size)
        assertSame(first, selected.single())
    }

    @Test
    fun `separate reads at the same path conflict even with identical bytes`() {
        write(tempDir.resolve("same.cjo"), "first")
        val first = assertNotNull(manager().loadPackageSnapshot(PACKAGE))
        val second = assertNotNull(manager().loadPackageSnapshot(PACKAGE))
        assertEquals(first.pkg.fullPkgName, second.pkg.fullPkgName)
        assertEquals(first.pkg.moduleName, second.pkg.moduleName)
        assertEquals(first.sourcePath, second.sourcePath)
        assertNotSame(first, second)
        assertFailsWith<IllegalStateException> {
            CjoExportSourceSnapshots.uniqueLoadedPackages(listOf(first, second))
        }
    }

    @Test
    fun `file replacement cannot update or combine an already loaded generation`() {
        val path = tempDir.resolve("changed.cjo")
        write(path, "before")
        val oldManager = manager()
        val before = assertNotNull(oldManager.loadPackageSnapshot(PACKAGE))
        write(path, "after")
        val after = assertNotNull(manager().loadPackageSnapshot(PACKAGE))
        assertSame(before, oldManager.loadedPackageSnapshot(path))
        assertSame(before, oldManager.loadPackageSnapshot(PACKAGE))
        assertEquals("before", before.pkg.moduleName)
        assertEquals("after", after.pkg.moduleName)
        assertFailsWith<IllegalStateException> {
            CjoExportSourceSnapshots.uniqueLoadedPackages(listOf(before, after))
        }
    }

    @Test
    fun `package accessors cannot mutate manager owned snapshot bytes`() {
        write(tempDir.resolve("readonly.cjo"), "first")
        val snapshot = assertNotNull(manager().loadPackageSnapshot(PACKAGE))
        assertFailsWith<ReadOnlyBufferException> {
            snapshot.pkg.byteBuffer.put(0, 0.toByte())
        }
        assertEquals(PACKAGE, snapshot.pkg.fullPkgName)
        assertEquals("first", snapshot.pkg.moduleName)
    }

    @Test
    fun `different source paths retain independent same package inputs`() {
        val firstPath = write(tempDir.resolve("first.cjo"), "first")
        val secondPath = write(tempDir.resolve("second.cjo"), "second")
        val manager = CjoManager(
            CjoSearchPath(envProvider = { null }), CjoFormatVersionPolicy.DeferredToConsumerSession,
            explicitCommonPartPaths = listOf(firstPath, secondPath),
        )
        val snapshots = manager.loadPackageSnapshots(PACKAGE)
        assertEquals(2, snapshots.size)
        val selected = CjoExportSourceSnapshots.uniqueLoadedPackages(snapshots)
        assertEquals(listOf("first", "second"), selected.map { it.pkg.moduleName })
        assertSame(snapshots[0], selected[0])
        assertSame(snapshots[1], selected[1])
    }

    private fun manager(): CjoManager = CjoManager(
        CjoSearchPath(additionalLibrarySearchPaths = listOf(tempDir.toFile()), envProvider = { null }),
        CjoFormatVersionPolicy.DeferredToConsumerSession,
    )

    private fun write(path: Path, module: String): Path = CjoPackageWriter.write(
        path, CjoPackageMetadata(fullPackageName = PACKAGE, moduleName = module, schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3),
    )

    private companion object {
        const val PACKAGE = "snapshot.identity"
    }
}
