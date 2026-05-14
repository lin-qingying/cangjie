package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.Package
import PackageFormat.PackageKind
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CjoPackageWriterTest {
    @Test
    fun `writes macro package metadata readable by package header`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "macros.pkg",
                moduleName = "macro-module",
                kind = PackageKind.Macro,
                version = "1.0.0",
                cjoVersion = CjoFormatVersion(1u, 2u, 3u),
                imports = listOf("std.core"),
                allFiles = listOf("src/macros/pkg/Macro.cj"),
                declarations = listOf(
                    CjoPackageDeclaration("Beta"),
                    CjoPackageDeclaration("Alpha", exportId = "macros.pkg.Alpha"),
                ),
            )
        )

        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val header = CjoPackageHeader.fromPackage(pkg)

        assertTrue(Package.PackageBufferHasIdentifier(ByteBuffer.wrap(bytes)))
        assertEquals("macros.pkg", header.fullPkgName)
        assertEquals("macro-module", header.moduleName)
        assertEquals(PackageKind.Macro, header.kind)
        assertEquals("1.0.0", pkg.version)
        assertEquals(1u, pkg.cjoVersion?.majorNum)
        assertEquals(2u, pkg.cjoVersion?.minorNum)
        assertEquals(3u, pkg.cjoVersion?.patchNum)
        assertEquals(listOf("std.core"), header.imports)
        assertEquals(listOf("src/macros/pkg/Macro.cj"), header.allFiles)
        assertEquals(listOf("Alpha", "Beta"), header.topLevelCallableNames.map { it.asString() }.sorted())
        assertEquals(2, header.topLevelNameToIndices.size)
        assertEquals(1, header.fullIdReferenceKeyToIndex["macros.pkg.Alpha"])
    }

    @Test
    fun `writes classifier declarations and file import metadata`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.pkg",
                moduleName = "sample",
                allFiles = listOf("src/sample/pkg/first.cj", "src/sample/pkg/second.cj"),
                fileImports = listOf(
                    CjoPackageFileImports(
                        imports = listOf(
                            CjoPackageImport(
                                prefixPaths = listOf("org", "sample"),
                                identifier = "Thing",
                                alias = "AliasThing",
                                hasDoubleColon = true,
                            ),
                            CjoPackageImport(
                                prefixPaths = listOf("sample", "star"),
                                identifier = "*",
                            ),
                        ),
                    ),
                ),
                declarations = listOf(
                    CjoPackageDeclaration("Box", kind = DeclKind.ClassDecl),
                    CjoPackageDeclaration("makeBox", kind = DeclKind.FuncDecl),
                ),
            )
        )

        val header = CjoPackageHeader.fromPackage(Package.getRootAsPackage(ByteBuffer.wrap(bytes)))

        assertEquals(listOf("org::sample.Thing as AliasThing", "sample.star.*"), header.decompiledImportTexts)
        assertEquals(setOf("Box"), header.topLevelClassNames.mapTo(mutableSetOf()) { it.asString() })
        assertEquals(setOf("makeBox"), header.topLevelCallableNames.mapTo(mutableSetOf()) { it.asString() })
    }

    @Test
    fun `writes cjo file to target path`() {
        val dir = Files.createTempDirectory("cjo-writer-test")
        val path = dir.resolve(Path.of("nested", "macro.cjo"))

        CjoPackageWriter.write(
            path,
            CjoPackageMetadata(
                fullPackageName = "written.pkg",
                moduleName = "written",
                kind = PackageKind.Macro,
                declarations = listOf(CjoPackageDeclaration("Generated")),
            ),
        )

        val header = CjoPackageHeader.fromPackage(Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(path))))
        assertEquals("written.pkg", header.fullPkgName)
        assertEquals(setOf("Generated"), header.topLevelCallableNames.mapTo(mutableSetOf()) { it.asString() })
    }
}
