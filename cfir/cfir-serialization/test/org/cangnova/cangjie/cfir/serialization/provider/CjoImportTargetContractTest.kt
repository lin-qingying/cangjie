package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.serialization.deserialize.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

/** imported 的真实 target/pool、mode 和完成事务必须一起消费，拒绝发生在目标物化之前。 */
class CjoImportTargetContractTest {
    @TempDir lateinit var directory: Path

    @Test fun wrongTargetModeIsRejectedBeforeEnteringTheDeclarationPool() {
        val f = CjoBindingGateFixture(directory)
        val targetFile = f.write("target.cjo", "gate.target", listOf(f.classDeclaration("Box", "gate.box")))
        val sourceFile = f.write("source.cjo", "gate.source", imports = listOf("gate.target"))
        val targetFrame = f.frame("target", setOf(targetFile))
        val target = f.context(targetFrame, targetFile, CjoDeserializationMode.SIGNATURE_ONLY)
        val sourceFrame = f.frame("source", setOf(sourceFile),
            router = CjoPackageContextProvider { _, _ -> target })
        val source = f.context(sourceFrame, sourceFile, CjoDeserializationMode.FULL)
        assertFailsWith<IllegalArgumentException> { source.fullIdResolver.resolveSymbol(f.importedReference("gate.box")) }
        assertTrue(target.declCache.isEmpty())
        assertTrue(target.typeCache.isEmpty())
        assertEquals(CjoDeserializationMode.SIGNATURE_ONLY, target.mode)
    }

    @Test fun wrongPackageHeaderCannotBeUsedWithTheRequestedImportIndex() {
        val f = CjoBindingGateFixture(directory)
        val targetFile = f.write("target.cjo", "gate.other", listOf(f.classDeclaration("Box", "gate.box")))
        val sourceFile = f.write("source.cjo", "gate.source", imports = listOf("gate.requested"))
        val target = f.context(f.frame("target", setOf(targetFile)), targetFile, CjoDeserializationMode.FULL)
        val source = f.context(f.frame("source", setOf(sourceFile),
            router = CjoPackageContextProvider { _, _ -> target }), sourceFile, CjoDeserializationMode.FULL)
        assertFailsWith<IllegalArgumentException> { source.fullIdResolver.resolveSymbol(f.importedReference("gate.box")) }
        assertTrue(target.declCache.isEmpty())
        assertTrue(target.typeCache.isEmpty())
    }

    @Test fun anotherGraphsTransactionIsRejectedBeforeTargetMaterialization() {
        val f = CjoBindingGateFixture(directory)
        val other = CjoBindingGateFixture(Files.createDirectory(directory.resolve("other-graph")))
        val targetFile = other.write("target.cjo", "gate.target", listOf(other.classDeclaration("Box", "gate.box")))
        val sourceFile = f.write("source.cjo", "gate.source", imports = listOf("gate.target"))
        val target = other.context(other.frame("other-owner", setOf(targetFile)), targetFile, CjoDeserializationMode.FULL)
        val source = f.context(f.frame("source", setOf(sourceFile),
            router = CjoPackageContextProvider { _, _ -> target }), sourceFile, CjoDeserializationMode.FULL)
        assertFailsWith<IllegalArgumentException> { source.fullIdResolver.resolveSymbol(f.importedReference("gate.box")) }
        assertTrue(target.declCache.isEmpty())
        assertTrue(target.typeCache.isEmpty())
        assertNotSame(source.materialization, target.materialization)
    }

    @Test fun fullIdIndexAndSymbolStayInTheSameAcceptedTargetEvenIfTheRouterLaterOffersAnotherSource() {
        val f = CjoBindingGateFixture(directory)
        val aFile = f.write("a.cjo", "gate.target", listOf(f.classDeclaration("Box", "gate.box"), f.classDeclaration("Filler")))
        val bFile = f.write("b.cjo", "gate.target", listOf(f.classDeclaration("Other"), f.classDeclaration("Box", "gate.box")))
        val sourceFile = f.write("source.cjo", "gate.source", imports = listOf("gate.target"))
        val aFrame = f.frame("owner-a", setOf(aFile))
        val bFrame = f.frame("owner-b", setOf(bFile))
        val a = f.context(aFrame, aFile, CjoDeserializationMode.FULL)
        val b = f.context(bFrame, bFile, CjoDeserializationMode.FULL)
        val reads = AtomicInteger()
        val sourceFrame = f.frame("source", setOf(sourceFile), router = CjoPackageContextProvider { name, mode ->
            check(name == "gate.target" && mode == CjoDeserializationMode.FULL)
            if (reads.getAndIncrement() == 0) a else b
        })
        val source = f.context(sourceFrame, sourceFile, CjoDeserializationMode.FULL)
        val reference = f.importedReference("gate.box")
        val decoded = assertIs<ResolvedFullId.Declaration>(source.fullIdResolver.resolve(reference))
        assertSame(a, decoded.targetContext)
        assertEquals(0, decoded.declaration.zeroBasedIndex)
        val symbol = source.fullIdResolver.resolveSymbol(reference)
        assertSame(aFrame.module, symbol.cfir.moduleData)
        assertEquals("Box", decoded.declaration.name.asString())
        assertIs<org.cangnova.cangjie.cfir.declarations.CfirClass>(symbol.cfir)
        assertSame<Any>(symbol, assertNotNull(a.classSymbolLookup.getClassLikeSymbolByClassId(
            ClassId(FqName("gate.target"), Name.identifier("Box")))))
        assertSame(symbol, source.fullIdResolver.resolveSymbol(reference))
        assertEquals(1, reads.get())
        assertTrue(b.declCache.isEmpty())
        assertSame(a, source.importedPackageIndices.getValue(0).context)
        assertSame(a.packageIndex, source.importedPackageIndices.getValue(0).packageIndex)
    }

    @Test fun validImportsShareOriginalNamedRecordsWithinEachModeAndKeepModesSeparate() {
        val f = CjoBindingGateFixture(directory)
        val file = f.wire("wire.current")
        val sourceFile = f.write("source.cjo", "gate.source", imports = listOf("wire.current"))
        val sdk = f.frame("sdk", setOf(f.sdk))
        val target = f.frame("target", setOf(file), router = sdk.provider)
        val source = f.frame("source", setOf(sourceFile), router = target.provider)
        val actualTarget = f.context(target, file, CjoDeserializationMode.FULL)
        val firstIndex = actualTarget.header.topLevelCallableNameToIndices.getValue("first").single()
        val key = actualTarget.pkg.allDecls(firstIndex)!!.exportId!!
        val symbols = CjoDeserializationMode.entries.map { mode ->
            val context = f.context(source, sourceFile, mode)
            val named = target.forMode(mode).getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single()
            val symbol = context.fullIdResolver.resolveSymbol(f.importedReference(key))
            assertSame(named, symbol)
            assertSame(target.module, symbol.cfir.moduleData)
            val accepted = context.importedPackageIndices.getValue(0).context
            assertEquals(mode, accepted.mode)
            assertEquals(file.toAbsolutePath().normalize(), accepted.sourcePath)
            assertSame(accepted, f.context(target, file, mode))
            symbol
        }
        assertNotSame(symbols[0], symbols[1])
    }
}
