package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.PackageKind
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class MacroArtifactResolverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun validMacroArtifactEmitsArtifactDefinitionsWithDynamicLibraryAndAbi() {
        val cjo = writeCjo("macro.cjo", "macros.pkg", PackageKind.Macro, listOf("Beta", "Alpha"))
        val dylib = writeFile("macro.dll")
        val bchir = writeFile("dep.bchir")

        val result = MacroArtifactResolver().resolve(
            listOf(
                artifact(
                    packageFqName = "macros.pkg",
                    cjoPath = cjo,
                    dynamicLibPath = dylib,
                    dependenciesBchirPaths = listOf(bchir),
                    abiVersion = "macro-abi-v1",
                )
            )
        )

        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        assertEquals(listOf("Alpha", "Beta"), result.definitions.map { it.name.asString() })
        assertTrue(result.definitions.all { it.source == MacroDefinitionEntry.Source.MACRO_ARTIFACT })
        assertTrue(result.definitions.all { it.libPath == dylib.toString() })
        assertTrue(result.definitions.all { it.executorAbi == "macro-abi-v1" })
        assertTrue(result.definitions.all { !it.artifactSignature.isNullOrBlank() })
        assertTrue(result.definitions.all { !it.cjoHash.isNullOrBlank() })
        assertTrue(result.definitions.all { !it.dynamicLibHash.isNullOrBlank() })
        assertTrue(result.definitions.all { !it.dependenciesBchirHash.isNullOrBlank() })
        assertTrue(result.definitions.all { it.resolverAlgorithmVersion == MacroArtifactResolver.ALGORITHM_VERSION })
    }

    @Test
    fun explicitArtifactSignatureIsPreservedForCacheKey() {
        val cjo = writeCjo("macro.cjo", "macros.pkg", PackageKind.Macro, listOf("Generated"))
        val dylib = writeFile("macro.dll")

        val result = MacroArtifactResolver().resolve(
            listOf(
                artifact(
                    packageFqName = "macros.pkg",
                    cjoPath = cjo,
                    dynamicLibPath = dylib,
                    signature = "orchestration-signature",
                )
            )
        )

        assertEquals("orchestration-signature", result.definitions.single().artifactSignature)
    }

    @Test
    fun missingArtifactFilesReportStructuredDiagnosticsAndEmitNoDefinitions() {
        val missingCjo = tempDir.resolve("missing.cjo")
        val missingDylib = tempDir.resolve("missing.dll")
        val missingBchir = tempDir.resolve("missing.bchir")

        val result = MacroArtifactResolver().resolve(
            listOf(
                artifact(
                    packageFqName = "macros.pkg",
                    cjoPath = missingCjo,
                    dynamicLibPath = missingDylib,
                    dependenciesBchirPaths = listOf(missingBchir),
                )
            )
        )

        assertTrue(result.definitions.isEmpty())
        assertEquals(
            setOf(
                MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE,
                MacroConstructionDiagnostic.Kind.MACRO_CANNOT_OPEN_LIB,
                MacroConstructionDiagnostic.Kind.MACRO_CANNOT_FIND_DEPENDENCY_BCHIR,
            ),
            result.diagnostics.mapTo(mutableSetOf()) { it.kind },
        )
        assertTrue(result.diagnostics.all { it.diagnosticOrigin == MacroConstructionDiagnostic.Origin.ARTIFACT_RESOLVER })
    }

    @Test
    fun orchestrationArtifactFailureKeepsCompileInvocationDiagnosticsReference() {
        val result = MacroArtifactResolver().resolve(
            listOf(
                artifact(
                    packageFqName = "macros.pkg",
                    cjoPath = tempDir.resolve("missing.cjo"),
                    dynamicLibPath = tempDir.resolve("missing.dll"),
                    compileInvocationId = "compile-macros.pkg",
                    sourceDiagnosticsRef = "diagnostics://macros.pkg",
                    origin = MacroArtifactPackage.Origin.ORCHESTRATION,
                )
            )
        )

        assertTrue(result.definitions.isEmpty())
        assertTrue(result.diagnostics.isNotEmpty())
        assertTrue(result.diagnostics.all { it.compileInvocationId == "compile-macros.pkg" })
        assertTrue(result.diagnostics.all { it.sourceDiagnosticsRef == "diagnostics://macros.pkg" })
    }

    @Test
    fun nonMacroPackageAndEmptyMacroPackageAreRejectedAsMacroDefinitionErrors() {
        val normalCjo = writeCjo("normal.cjo", "macros.normal", PackageKind.Normal, listOf("NotMacro"))
        val emptyMacroCjo = writeCjo("empty.cjo", "macros.empty", PackageKind.Macro, emptyList())
        val dylib = writeFile("macro.dll")

        val result = MacroArtifactResolver().resolve(
            listOf(
                artifact("macros.normal", normalCjo, dylib),
                artifact("macros.empty", emptyMacroCjo, dylib),
            )
        )

        assertTrue(result.definitions.isEmpty())
        assertEquals(
            listOf(
                MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
            ),
            result.diagnostics.map { it.kind },
        )
    }

    @Test
    fun packageNameMismatchReportsUndefinedPackageAndSkipsDefinitions() {
        val cjo = writeCjo("macro.cjo", "actual.pkg", PackageKind.Macro, listOf("Generated"))
        val dylib = writeFile("macro.dll")

        val result = MacroArtifactResolver().resolve(
            listOf(artifact("expected.pkg", cjo, dylib))
        )

        assertTrue(result.definitions.isEmpty())
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE, result.diagnostics.single().kind)
        assertEquals(FqName("expected.pkg"), result.diagnostics.single().artifactPackage)
        assertEquals(cjo.toString(), result.diagnostics.single().artifactPath)
    }

    @Test
    fun abiMismatchReportsArtifactErrorAndSkipsDefinitions() {
        val cjo = writeCjo("macro.cjo", "macros.pkg", PackageKind.Macro, listOf("Generated"))
        val dylib = writeFile("macro.dll")

        val result = MacroArtifactResolver().resolve(
            packages = listOf(
                artifact(
                    packageFqName = "macros.pkg",
                    cjoPath = cjo,
                    dynamicLibPath = dylib,
                    abiVersion = "artifact-abi-v2",
                )
            ),
            expectedExecutorAbiVersion = "executor-abi-v1",
        )

        assertTrue(result.definitions.isEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION, diagnostic.kind)
        assertEquals(MacroConstructionDiagnostic.Origin.ARTIFACT_RESOLVER, diagnostic.diagnosticOrigin)
        assertTrue(
            diagnostic.message.contains("artifact-abi-v2") &&
                diagnostic.message.contains("executor-abi-v1"),
            "ABI mismatch diagnostic must include both artifact and executor ABI: ${diagnostic.message}",
        )
    }

    private fun artifact(
        packageFqName: String,
        cjoPath: Path,
        dynamicLibPath: Path,
        dependenciesBchirPaths: List<Path> = emptyList(),
        abiVersion: String? = null,
        signature: String? = null,
        origin: MacroArtifactPackage.Origin = MacroArtifactPackage.Origin.EXTERNAL_PATH,
        compileInvocationId: String? = null,
        sourceDiagnosticsRef: String? = null,
    ): MacroArtifactPackage = MacroArtifactPackage(
        packageFqName = FqName(packageFqName),
        kind = MacroArtifactPackage.Kind.MACRO,
        cjoPath = cjoPath.toString(),
        dynamicLibPath = dynamicLibPath.toString(),
        dependenciesBchirPaths = dependenciesBchirPaths.map(Path::toString),
        abiVersion = abiVersion,
        signature = signature,
        origin = origin,
        compileInvocationId = compileInvocationId,
        sourceDiagnosticsRef = sourceDiagnosticsRef,
    )

    private fun writeFile(name: String): Path {
        val path = tempDir.resolve(name)
        Files.write(path, byteArrayOf(1))
        return path
    }

    private fun writeCjo(
        name: String,
        packageFqName: String,
        kind: UByte,
        callableNames: List<String>,
    ): Path {
        val path = tempDir.resolve(name)
        return CjoPackageWriter.write(
            path,
            CjoPackageMetadata(
                fullPackageName = packageFqName,
                moduleName = "macro-test",
                kind = kind,
                declarations = callableNames.map(::CjoPackageDeclaration),
            ),
        )
    }
}
