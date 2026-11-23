package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/** 旧九参数入口保留；源/服务 authority 由原 producer 确认，实际查询不能重选 named 文件。 */
class CjoContextAuthorityTest {
    @TempDir lateinit var directory: Path

    @Test fun legacyNineArgumentLookupStaysInItsActualSnapshotInBothPublicationOrders() {
        for (legacyFirst in listOf(true, false)) {
            val root = Files.createDirectory(directory.resolve("order-" + legacyFirst))
            val f = CjoBindingGateFixture(root)
            val namedFile = f.write("00-named.cjo", "gate.same", listOf(f.classDeclaration("NamedBox")))
            val actualFile = f.write("99-actual.cjo", "gate.same", listOf(f.classDeclaration("ActualBox")))
            val frame = f.frame("actual-owner", setOf(root))
            val named = assertNotNull(frame.provider.getOwnPackageContext("gate.same", CjoDeserializationMode.FULL))
            assertEquals(namedFile.toAbsolutePath().normalize(), named.sourcePath)
            val snapshot = frame.manager.loadPackageSnapshot("gate.same", actualFile, Files.readAllBytes(actualFile))
            fun legacy() = CfirDeserializationContext(
                snapshot.pkg, snapshot.header, frame.module, frame.manager, frame.provider,
                snapshot.sourcePath, emptySet(), CjoDeserializationMode.FULL, frame.provider,
            ).canonical()
            val first = if (legacyFirst) legacy() else frame.binding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL)
            val second = if (legacyFirst) frame.binding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL) else legacy()
            assertSame(first, second)
            assertSame(snapshot, first.snapshotIdentity)
            assertEquals(actualFile.toAbsolutePath().normalize(), first.sourcePath)
            val id = ClassId(FqName("gate.same"), Name.identifier("ActualBox"))
            val symbol = assertNotNull(first.classSymbolLookup.getClassLikeSymbolByClassId(id))
            assertIs<CfirClass>(symbol.cfir)
            assertSame(frame.module, symbol.cfir.moduleData)
            assertSame(symbol, second.classSymbolLookup.getClassLikeSymbolByClassId(id))
            assertNull(first.classSymbolLookup.getClassLikeSymbolByClassId(ClassId(FqName("gate.same"), Name.identifier("NamedBox"))))
            assertNotSame(first, named)
            assertNotNull(named.classSymbolLookup.getClassLikeSymbolByClassId(ClassId(FqName("gate.same"), Name.identifier("NamedBox"))))
        }
    }

    @Test fun anUnregisteredLegacyRouterCannotWinCanonicalPublication() {
        val f = CjoBindingGateFixture(directory)
        val file = f.write("source.cjo", "gate.source", listOf(f.classDeclaration("Box")))
        val frame = f.frame("router-owner", setOf(file))
        val snapshot = frame.manager.loadPackageSnapshot("gate.source", file, Files.readAllBytes(file))
        val rejecting = CjoPackageContextProvider { _, _ -> null }
        repeat(2) {
            assertFailsWith<IllegalArgumentException> {
                CfirDeserializationContext(snapshot.pkg, snapshot.header, frame.module, frame.manager, frame.provider,
                    file, emptySet(), CjoDeserializationMode.FULL, rejecting)
            }
            val actual = frame.binding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL)
            assertSame(actual, frame.binding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL))
            assertSame(frame.binding, actual.packageContextProvider)
        }
    }

    @Test fun anOriginalRejectingRouterSurvivesAnotherBindingsWarmCanonicalContext() {
        val f = CjoBindingGateFixture(directory)
        val source = f.write("source.cjo", "gate.source")
        val target = f.write("target.cjo", "gate.target", listOf(f.classDeclaration("Other")))
        val shared = f.manager(source)
        val moduleFrame = f.frame("shared-owner", setOf(source), shared)
        val snapshot = shared.loadPackageSnapshot("gate.source", source, Files.readAllBytes(source))
        val warm = moduleFrame.binding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL)
        val dependency = f.manager(target)
        assertNotNull(dependency.loadPackageSnapshot("gate.target"))
        val original = CfirDeserializedSymbolProvider(moduleFrame.module.session, shared, moduleFrame.module.session.cangjieScopeProvider, moduleFrame.module,
            moduleDataProvider = org.cangnova.cangjie.test.cjoFixtureModuleDataProvider(moduleFrame.module, setOf(source)),
            dependencyCjoManager = dependency,
            importedPackageContextProvider = CjoPackageContextProvider { _, _ -> null })
        val exact = CfirDeserializationContext(snapshot.pkg, snapshot.header, moduleFrame.module, shared, original,
            source, emptySet(), CjoDeserializationMode.FULL, original).canonical()
        assertNotSame(warm, exact)
        assertNotSame(warm.binding, exact.binding)
        assertNull(exact.packageContextProvider.getPackageContext("gate.target", CjoDeserializationMode.FULL))
        assertNotNull(dependency.loadPackageSnapshot("gate.target"))
        assertSame(exact, original.deserializationBinding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL))
    }

    @Test fun lookupFromAnotherOriginalBindingIsRejectedBeforeRecordsArePublished() {
        val f = CjoBindingGateFixture(directory)
        val file = f.write("source.cjo", "gate.source", listOf(f.classDeclaration("Box")))
        val a = f.frame("owner-a", setOf(file))
        val b = f.frame("owner-b", setOf(file), a.manager)
        val snapshot = a.manager.loadPackageSnapshot("gate.source", file, Files.readAllBytes(file))
        assertFailsWith<IllegalArgumentException> {
            CfirDeserializationContext(snapshot.pkg, snapshot.header, a.module, a.manager, b.provider,
                sourcePath = file, binding = a.binding, snapshotIdentity = snapshot)
        }
        assertTrue(a.binding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL).declCache.isEmpty())
    }

    @Test fun sourceHeaderAndPoolCannotBeCombinedFromDifferentActualFiles() {
        val f = CjoBindingGateFixture(directory)
        val aFile = f.write("a.cjo", "gate.same", listOf(f.classDeclaration("A")))
        val bFile = f.write("b.cjo", "gate.same", listOf(f.classDeclaration("B")))
        val frame = f.frame("pool-owner", setOf(directory))
        val a = frame.manager.loadPackageSnapshot("gate.same", aFile, Files.readAllBytes(aFile))
        val b = frame.manager.loadPackageSnapshot("gate.same", bFile, Files.readAllBytes(bFile))
        assertFailsWith<IllegalArgumentException> {
            CfirDeserializationContext(b.pkg, a.header, frame.module, frame.manager, frame.provider,
                sourcePath = aFile, binding = frame.binding, snapshotIdentity = a)
        }
        val actual = frame.binding.contextForSnapshot(a, CjoDeserializationMode.FULL)
        assertTrue(actual.declCache.isEmpty())
        assertNotNull(actual.classSymbolLookup.getClassLikeSymbolByClassId(ClassId(FqName("gate.same"), Name.identifier("A"))))
    }

    @Test fun aDerivedProviderCannotSilentlyIgnoreAnotherImportRouterArgument() {
        val f = CjoBindingGateFixture(directory)
        val file = f.write("source.cjo", "gate.source")
        val frame = f.frame("derived-owner", setOf(file))
        assertFailsWith<IllegalArgumentException> {
            CfirDeserializedSymbolProvider(frame.module.session, frame.manager, frame.binding.scopeProvider, frame.module,
                moduleDataProvider = frame.binding.moduleDataProvider,
                importedPackageContextProvider = CjoPackageContextProvider { _, _ -> null },
                binding = frame.binding)
        }
        assertSame(frame.provider, frame.manager.deserializedSymbolLookups.forBinding(frame.binding, frame.module, CjoDeserializationMode.FULL))
    }

    @Test fun legacySystemAnnotationConfigurationMustComeFromTheOriginalSession() {
        val f = CjoBindingGateFixture(directory)
        val file = f.write("source.cjo", "gate.source")
        val frame = f.frame("config-owner", setOf(file))
        val snapshot = frame.manager.loadPackageSnapshot("gate.source", file, Files.readAllBytes(file))
        val authorized = setOf(FqName("ohos.labels.APILevel"))
        assertFailsWith<IllegalArgumentException> {
            CfirDeserializationContext(snapshot.pkg, snapshot.header, frame.module, frame.manager, frame.provider,
                file, authorized, CjoDeserializationMode.FULL, frame.provider)
        }
        val configured = f.frame("configured-owner", setOf(file), frame.manager, implicitAnnotations = authorized)
        val exact = CfirDeserializationContext(snapshot.pkg, snapshot.header, configured.module, configured.manager, configured.provider,
            file, authorized, CjoDeserializationMode.FULL, configured.provider).canonical()
        assertEquals(authorized, exact.implicitSystemAnnotations)
        assertSame(exact, configured.binding.contextForSnapshot(snapshot, CjoDeserializationMode.FULL))
    }
}
