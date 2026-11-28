package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.ReferenceInfo
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticKind
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.test.bindCjoFixture
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** 真实 CJO、真实 manager/binding；同时核对来源身份、模式与原 wire 编辑接口。 */
class CjoSourceSnapshotIdentityTest {
    @Test
    fun concurrentNamedAndActualReadersShareSourceAndCanonicalContext() {
        repeat(16) { withFixture { f ->
            val start = CyclicBarrier(3)
            val pool = Executors.newFixedThreadPool(3)
            try {
                val named = pool.submit<CjoLoadedPackage> {
                    start.await(10, TimeUnit.SECONDS)
                    assertNotNull(f.manager.loadPackageSnapshot(NAME))
                }
                val actual = pool.submit<CjoLoadedPackage> {
                    start.await(10, TimeUnit.SECONDS)
                    f.manager.loadPackageSnapshot(NAME, f.source, f.bytes)
                }
                val anotherNamed = pool.submit<CjoLoadedPackage> {
                    start.await(10, TimeUnit.SECONDS)
                    assertNotNull(f.manager.loadPackageSnapshot(NAME))
                }
                val snapshot = named.get(20, TimeUnit.SECONDS)
                assertSame(snapshot, actual.get(20, TimeUnit.SECONDS))
                assertSame(snapshot, anotherNamed.get(20, TimeUnit.SECONDS))
                val full = f.binding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL)
                val signature = f.binding.contextForSnapshot(snapshot, CjoDeserializationMode.SIGNATURE_ONLY)
                assertSame(full, f.binding.contextForSnapshot(actual.get(), CjoDeserializationMode.FULL))
                assertSame(signature, f.binding.contextForSnapshot(anotherNamed.get(), CjoDeserializationMode.SIGNATURE_ONLY))
                assertNotSame(full, signature)
                assertSame(snapshot, full.snapshotIdentity)
                assertSame(snapshot, signature.snapshotIdentity)
                assertSame(f.module, full.moduleData)
                assertEquals(f.source.toAbsolutePath().normalize(), full.sourcePath)
            } finally {
                pool.shutdownNow()
                assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
            }
        } }
    }

    @Test
    fun namedAndActualFirstOrderShareTheSameObject() {
        for (actualFirst in listOf(false, true)) withFixture { f ->
            val first = if (actualFirst) f.manager.loadPackageSnapshot(NAME, f.source, f.bytes)
                else assertNotNull(f.manager.loadPackageSnapshot(NAME))
            val second = if (actualFirst) assertNotNull(f.manager.loadPackageSnapshot(NAME))
                else f.manager.loadPackageSnapshot(NAME, f.source, f.bytes)
            assertSame(first, second)
            assertSame(f.binding.contextForSnapshot(first, CjoDeserializationMode.FULL),
                f.binding.contextForSnapshot(second, CjoDeserializationMode.FULL))
        }
    }

    @Test
    fun wireWorkingEditsDoNotChangeTheRecordedSourceBytes() = withFixture { f ->
        val actual = f.manager.loadPackageSnapshot(NAME, f.source, f.bytes)
        val targetIndex = (actual.pkg.allExprs(1)!!.info(ReferenceInfo()) as ReferenceInfo).target!!.index
        editReference(actual.pkg.byteBuffer)
        assertSame(actual, f.manager.loadPackageSnapshot(NAME, f.source, f.bytes))
        assertSame(actual, f.manager.loadPackageSnapshot(NAME))
        val reference = actual.pkg.allExprs(1)!!.info(ReferenceInfo()) as ReferenceInfo
        assertEquals("other", reference.reference)
        assertEquals(targetIndex, reference.target!!.index)
    }

    @Test
    fun actualReaderRejectsChangedContentWithoutReplacingTheNamedSnapshot() = withFixture { f ->
        val first = assertNotNull(f.manager.loadPackageSnapshot(NAME))
        val changed = f.bytes.copyOf()
        editReference(ByteBuffer.wrap(changed))
        assertFailsWith<IllegalStateException> { f.manager.loadPackageSnapshot(NAME, f.source, changed) }
        assertSame(first, f.manager.loadPackageSnapshot(NAME))
        assertSame(first, f.manager.loadPackageSnapshot(NAME, f.source, f.bytes))
    }

    @Test
    fun firstNamedReadChecksTheSameGenerationDiskContent() = withFixture { f ->
        val first = f.manager.loadPackageSnapshot(NAME, f.source, f.bytes)
        val changed = f.bytes.copyOf()
        editReference(ByteBuffer.wrap(changed))
        Files.write(f.source, changed)
        assertFailsWith<IllegalStateException> { f.manager.loadPackageSnapshot(NAME) }
        assertSame(first, f.manager.loadPackageSnapshot(NAME, f.source, f.bytes))
    }

    @Test
    fun sourceContentIsCopiedBeforeCallerMutation() = withFixture { f ->
        val input = f.bytes.copyOf()
        val snapshot = f.manager.loadPackageSnapshot(NAME, f.source, input)
        editReference(ByteBuffer.wrap(input))
        assertSame(snapshot, f.manager.loadPackageSnapshot(NAME, f.source, f.bytes))
        assertFailsWith<IllegalStateException> { f.manager.loadPackageSnapshot(NAME, f.source, input) }
    }

    @Test
    fun wrongPackageDiagnosticsDoNotReplaceAnAcceptedSourceSnapshot() = withFixture { f ->
        val requested = "wire.wrong"
        val path = f.directory.resolve(org.cangnova.cangjie.cfir.serialization.CjoConstants.packageNameToPath(requested))
        Files.createDirectories(path.parent)
        Files.write(path, f.bytes)
        val first = f.manager.loadPackageSnapshot(NAME, path, f.bytes)
        assertNull(f.manager.loadPackageSnapshot(requested))
        assertEquals(CfirCjmpLoadDiagnosticKind.WRONG_PACKAGE, f.manager.loadDiagnostics(requested).single().kind)
        assertSame(first, f.manager.loadPackageSnapshot(NAME, path, f.bytes))
        assertNull(f.manager.loadPackageSnapshot(requested))
    }

    @Test
    fun versionDiagnosticsRemainOnTheNamedEntry() = withFixture { f ->
        val bytes = f.bytes.copyOf()
        val pkg = PackageFormat.Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        assertNotNull(pkg.cjoVersion).mutateMajorNum(255u)
        Files.write(f.source, bytes)
        assertNull(f.manager.loadPackageSnapshot(NAME))
        assertEquals(CfirCjmpLoadDiagnosticKind.CJO_VERSION, f.manager.loadDiagnostics(NAME).single().kind)
        assertFailsWith<IllegalStateException> { f.manager.loadPackageSnapshot(NAME, f.source, bytes) }
    }

    private fun editReference(buffer: ByteBuffer) {
        val pkg = PackageFormat.Package.getRootAsPackage(buffer)
        val info = pkg.allExprs(1)!!.info(ReferenceInfo()) as ReferenceInfo
        val spelling = info.referenceAsByteBuffer
        "other".toByteArray(Charsets.UTF_8).forEachIndexed { index, value ->
            spelling.put(spelling.position() + index, value)
        }
    }

    private fun withFixture(action: (Fixture) -> Unit) {
        val directory = Files.createTempDirectory("cjo-source-identity-")
        try {
            val text = checkNotNull(javaClass.classLoader.getResourceAsStream("cjo-wire/wire.current.cjo.b64"))
                .use { it.reader(Charsets.UTF_8).readText().trim() }
            val bytes = Base64.getDecoder().decode(text)
            val source = Files.write(directory.resolve("wire.current.cjo"), bytes)
            val manager = CjoManager(CjoSearchPath.forRoots(emptyList(), listOf(directory.toFile())))
            val module = Module()
            val binding = bindCjoFixture(module, manager, setOf(directory))
            action(Fixture(directory, source, bytes, manager, module, binding))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private data class Fixture(
        val directory: Path,
        val source: Path,
        val bytes: ByteArray,
        val manager: CjoManager,
        val module: CfirModuleData,
        val binding: org.cangnova.cangjie.cfir.serialization.provider.CjoDeserializationBinding,
    )

    private class Module : CfirModuleData() {
        override val name = Name.identifier("cjo-source-identity")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjo-source-identity"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class,
                CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
        }
        init { bindSession(session) }
    }

    companion object { private const val NAME = "wire.current" }
}
