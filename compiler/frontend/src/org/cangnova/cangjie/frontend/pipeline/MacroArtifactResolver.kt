package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.Package
import PackageFormat.PackageKind
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.utils.StableHash
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * 已编译宏包 artifact。
 *
 * Phase 1 只消费已经由独立 `--compile-macro` invocation 产出的 `.cjo + 动态库`，
 * 不在当前使用方 frontend invocation 中调度源码宏包编译。
 */
data class MacroArtifactPackage(
    val packageFqName: FqName,
    val kind: Kind,
    val cjoPath: String,
    val dynamicLibPath: String,
    val dependenciesBchirPaths: List<String> = emptyList(),
    val abiVersion: String? = null,
    val signature: String? = null,
    val origin: Origin,
    /** 产生该 artifact 的独立宏包编译 invocation。 */
    val compileInvocationId: String? = null,
    /** 独立宏包编译原始诊断引用，由 orchestration/CLI 层维护生命周期。 */
    val sourceDiagnosticsRef: String? = null,
) {
    enum class Kind {
        MACRO,
    }

    enum class Origin {
        SDK_STDLIB,
        EXTERNAL_PATH,
        ORCHESTRATION,
    }
}

data class MacroArtifactResolverResult(
    val definitions: List<MacroDefinitionEntry>,
    val diagnostics: List<MacroConstructionDiagnostic>,
) {
    val hasErrors: Boolean
        get() = diagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }
}

/**
 * 已编译宏包 resolver。
 *
 * 职责仅限发现/校验 artifact 并把 `.cjo` 中的 macro package 导出名称投影成
 * [MacroDefinitionEntry.Source.MACRO_ARTIFACT]；不加载动态库、不执行宏、不编译源码宏包。
 */
class MacroArtifactResolver {
    companion object {
        /** Artifact resolver 算法版本；搜索路径、校验规则或签名算法变化时必须递增。 */
        const val ALGORITHM_VERSION: Int = 1
    }

    fun resolve(
        packages: List<MacroArtifactPackage>,
        expectedExecutorAbiVersion: String? = null,
    ): MacroArtifactResolverResult {
        val definitions = mutableListOf<MacroDefinitionEntry>()
        val diagnostics = mutableListOf<MacroConstructionDiagnostic>()

        for (artifact in packages) {
            val packageDiagnostics = validateFiles(artifact, expectedExecutorAbiVersion)
            diagnostics += packageDiagnostics
            if (packageDiagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }) continue

            val cjoFile = File(artifact.cjoPath)
            val header = runCatching { readCjoHeader(cjoFile) }.getOrElse { error ->
                diagnostics += artifact.error(
                    kind = MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                    message = "Cannot read macro artifact `.cjo` `${artifact.cjoPath}`: ${error.message.orEmpty()}",
                    artifactPath = artifact.cjoPath,
                )
                continue
            }

            if (header.fullPkgName != artifact.packageFqName.asString()) {
                diagnostics += artifact.error(
                    kind = MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE,
                    message = "Macro artifact `${artifact.cjoPath}` contains package `${header.fullPkgName}`, expected `${artifact.packageFqName.asString()}`.",
                    artifactPath = artifact.cjoPath,
                )
                continue
            }
            if (header.kind != PackageKind.Macro) {
                diagnostics += artifact.error(
                    kind = MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                    message = "Package `${artifact.packageFqName.asString()}` is not a macro package artifact.",
                    artifactPath = artifact.cjoPath,
                )
                continue
            }

            val macroNames = header.topLevelCallableNames.sortedBy { it.asString() }
            if (macroNames.isEmpty()) {
                diagnostics += artifact.error(
                    kind = MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                    message = "Macro package `${artifact.packageFqName.asString()}` contains no exported macro definitions.",
                    artifactPath = artifact.cjoPath,
                )
                continue
            }

            val cjoHash = hashFile(artifact.cjoPath)
            val dynamicLibHash = hashFile(artifact.dynamicLibPath)
            val bchirHash = StableHash.sha256Of(artifact.dependenciesBchirPaths.map(::hashFile))
            val artifactSignature = artifact.signature ?: StableHash.sha256Of(
                artifact.packageFqName.asString(),
                artifact.cjoPath,
                cjoHash,
                artifact.dynamicLibPath,
                dynamicLibHash,
                bchirHash,
                artifact.abiVersion.orEmpty(),
                artifact.origin.name,
                ALGORITHM_VERSION.toString(),
            )

            definitions += macroNames.map { macroName ->
                MacroDefinitionEntry(
                    packageFqName = artifact.packageFqName,
                    name = macroName,
                    source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                    libPath = artifact.dynamicLibPath,
                    executorAbi = artifact.abiVersion,
                    artifactSignature = artifactSignature,
                    cjoHash = cjoHash,
                    dynamicLibHash = dynamicLibHash,
                    dependenciesBchirHash = bchirHash,
                    resolverAlgorithmVersion = ALGORITHM_VERSION,
                )
            }
        }

        return MacroArtifactResolverResult(definitions, diagnostics)
    }

    private fun validateFiles(
        artifact: MacroArtifactPackage,
        expectedExecutorAbiVersion: String?,
    ): List<MacroConstructionDiagnostic> {
        val diagnostics = mutableListOf<MacroConstructionDiagnostic>()
        if (!File(artifact.cjoPath).isFile) {
            diagnostics += artifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE,
                message = "Cannot find macro artifact `.cjo` for package `${artifact.packageFqName.asString()}`: ${artifact.cjoPath}",
                artifactPath = artifact.cjoPath,
            )
        }
        if (!File(artifact.dynamicLibPath).isFile) {
            diagnostics += artifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_CANNOT_OPEN_LIB,
                message = "Cannot find macro dynamic library for package `${artifact.packageFqName.asString()}`: ${artifact.dynamicLibPath}",
                macroLibraryPath = artifact.dynamicLibPath,
            )
        }
        for (bchirPath in artifact.dependenciesBchirPaths) {
            if (!File(bchirPath).isFile) {
                diagnostics += artifact.error(
                    kind = MacroConstructionDiagnostic.Kind.MACRO_CANNOT_FIND_DEPENDENCY_BCHIR,
                    message = "Cannot find macro dependency BCHIR for package `${artifact.packageFqName.asString()}`: $bchirPath",
                    artifactPath = bchirPath,
                )
            }
        }
        if (
            !artifact.abiVersion.isNullOrBlank() &&
            !expectedExecutorAbiVersion.isNullOrBlank() &&
            artifact.abiVersion != expectedExecutorAbiVersion
        ) {
            diagnostics += artifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                message = "Macro artifact `${artifact.cjoPath}` ABI `${artifact.abiVersion}` does not match executor ABI `$expectedExecutorAbiVersion`.",
                artifactPath = artifact.cjoPath,
            )
        }
        return diagnostics
    }

    private fun readCjoHeader(file: File): CjoPackageHeader {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.allocate(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return CjoPackageHeader.fromPackage(Package.getRootAsPackage(buffer))
    }

    private fun hashFile(path: String): String {
        val file = File(path)
        return MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun MacroArtifactPackage.error(
        kind: MacroConstructionDiagnostic.Kind,
        message: String,
        artifactPath: String? = null,
        macroLibraryPath: String? = null,
    ): MacroConstructionDiagnostic {
        return MacroConstructionDiagnostic(
            severity = MacroConstructionDiagnostic.Severity.ERROR,
            message = message,
            kind = kind,
            artifactPackage = packageFqName,
            artifactPath = artifactPath,
            macroLibraryPath = macroLibraryPath,
            diagnosticOrigin = MacroConstructionDiagnostic.Origin.ARTIFACT_RESOLVER,
            compileInvocationId = compileInvocationId,
            sourceDiagnosticsRef = sourceDiagnosticsRef,
        )
    }
}
