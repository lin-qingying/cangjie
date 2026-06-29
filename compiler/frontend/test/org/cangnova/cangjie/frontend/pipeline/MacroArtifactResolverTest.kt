package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.PackageKind
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageFileImports
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageImport
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * 覆盖宏 artifact resolver 的文件校验、定义投影和缓存签名规则。
 */
class MacroArtifactResolverTest {
    /**
     * 每个测试独占的 artifact 临时目录。
     */
    @TempDir
    lateinit var tempDir: Path

    /**
     * 验证合法宏 artifact 会按导出名称生成 artifact 宏定义。
     */
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
        assertTrue(result.definitions.all { !it.dependenciesArtifactHash.isNullOrBlank() })
        assertTrue(result.definitions.all { it.resolverAlgorithmVersion == MacroArtifactResolver.ALGORITHM_VERSION })
    }

    /**
     * 验证 orchestration 提供的显式 artifact 签名会原样进入定义。
     */
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

    /**
     * 验证缺失 `.cjo`、动态库和 BCHIR 时产生结构化诊断且不生成定义。
     */
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

    /**
     * 验证 orchestration artifact 失败诊断保留 invocation 和原始诊断引用。
     */
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

    /**
     * 验证普通包和空宏包都会被拒绝为宏定义错误。
     */
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

    /**
     * 验证 `.cjo` 内包名与期望包名不一致时报 undefined package。
     */
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

    /**
     * 验证 artifact ABI 与 executor ABI 不一致时拒绝定义。
     */
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

    /**
     * 验证 public import 重导出的宏定义可见且使用真实 executable artifact。
     */
    @Test
    fun publicImportReexportedMacroDefinitionsAreVisibleToResolver() {
        val executablePackage = "upstream.deriving"
        writeCjo(
            "${toCjoFileName(FqName(executablePackage))}.cjo",
            executablePackage,
            PackageKind.Macro,
            listOf("Derive"),
        )
        val executableDylib = writeFile("lib-macro_${toCjoFileName(FqName(executablePackage))}.${dynamicLibraryExtension()}")
        val facadeCjo = writeCjo(
            name = "facade.cjo",
            packageFqName = "a",
            kind = PackageKind.Macro,
            callableNames = emptyList(),
            fileImports = listOf(
                CjoPackageFileImports(
                    listOf(
                        CjoPackageImport(
                            prefixPaths = listOf("upstream", "deriving"),
                            identifier = "Derive",
                            isDecl = true,
                            withImplicitExport = true,
                        ),
                    ),
                ),
            ),
        )
        val dylib = writeFile("macro.dll")

        val result = MacroArtifactResolver().resolve(
            packages = listOf(artifact("a", facadeCjo, dylib)),
            searchRoots = listOf(tempDir.toString()),
        )

        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        assertEquals(listOf("Derive"), result.definitions.map { it.name.asString() })
        assertEquals(FqName("a"), result.definitions.single().packageFqName)
        assertEquals(FqName(executablePackage), result.definitions.single().executablePackageFqName)
        assertEquals("Derive", result.definitions.single().executableName.asString())
        assertEquals(executableDylib.toString(), result.definitions.single().libPath)
    }

    /**
     * 构造测试用宏 artifact 包。
     */
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

    /**
     * 写入测试用普通二进制文件。
     */
    private fun writeFile(name: String): Path {
        val path = tempDir.resolve(name)
        Files.write(path, byteArrayOf(1))
        return path
    }

    /**
     * 写入带指定包元数据的 `.cjo` 文件。
     */
    private fun writeCjo(
        name: String,
        packageFqName: String,
        kind: UByte,
        callableNames: List<String>,
        fileImports: List<CjoPackageFileImports> = emptyList(),
    ): Path {
        val path = tempDir.resolve(name)
        return CjoPackageWriter.write(
            path,
            CjoPackageMetadata(
                fullPackageName = packageFqName,
                moduleName = "macro-test",
                fileImports = fileImports,
                kind = kind,
                declarations = callableNames.map(::CjoPackageDeclaration),
            ),
        )
    }
}
