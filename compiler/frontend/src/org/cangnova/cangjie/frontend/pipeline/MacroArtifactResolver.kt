package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.Package
import PackageFormat.PackageKind
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportedTopLevelNamesResolver
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
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
        const val ALGORITHM_VERSION: Int = 2
    }

    fun resolve(
        packages: List<MacroArtifactPackage>,
        expectedExecutorAbiVersion: String? = null,
        searchRoots: List<String> = emptyList(),
        sdkHome: String = DEFAULT_MACRO_SDK_HOME,
    ): MacroArtifactResolverResult {
        val definitions = mutableListOf<MacroDefinitionEntry>()
        val diagnostics = mutableListOf<MacroConstructionDiagnostic>()
        val exportedTopLevelNamesResolver = buildExportedTopLevelNamesResolver(packages, searchRoots)
        val artifactLocator = MacroArtifactLocator(sdkHome = sdkHome)
        val providedArtifactsByPackage = packages.associateBy(MacroArtifactPackage::packageFqName)
        val executableArtifactCache = mutableMapOf<FqName, MacroArtifactPackage?>()
        val fileHashCache = mutableMapOf<String, String>()

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

            val exportedMacros = resolveExportedMacros(
                artifact = artifact,
                header = header,
                exportedTopLevelNamesResolver = exportedTopLevelNamesResolver,
            )
            if (exportedMacros.isEmpty()) {
                diagnostics += artifact.error(
                    kind = MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                    message = "Macro package `${artifact.packageFqName.asString()}` contains no exported macro definitions.",
                    artifactPath = artifact.cjoPath,
                )
                continue
            }

            val visibleCjoHash = hashFile(artifact.cjoPath, fileHashCache)
            val visibleDynamicLibHash = hashFile(artifact.dynamicLibPath, fileHashCache)

            definitions += exportedMacros.mapNotNull { exportedMacro ->
                val executableArtifact = resolveExecutableArtifact(
                    exportedMacro = exportedMacro,
                    ownerArtifact = artifact,
                    providedArtifactsByPackage = providedArtifactsByPackage,
                    artifactLocator = artifactLocator,
                    searchRoots = searchRoots,
                    cache = executableArtifactCache,
                ) ?: run {
                    diagnostics += artifact.error(
                        kind = MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE,
                        message = "Re-exported macro `${macroFqName(artifact.packageFqName, exportedMacro.visibleName).asString()}` resolves to `${exportedMacro.executableFqName.asString()}`, but the executable macro artifact package `${exportedMacro.executablePackageFqName.asString()}` was not found.",
                        relatedTargets = listOf(exportedMacro.executableFqName),
                    )
                    return@mapNotNull null
                }

                val executionDiagnostics = validateExecutableArtifact(
                    ownerArtifact = artifact,
                    ownerMacro = exportedMacro,
                    executableArtifact = executableArtifact,
                    expectedExecutorAbiVersion = expectedExecutorAbiVersion,
                )
                if (executionDiagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }) {
                    diagnostics += executionDiagnostics
                    return@mapNotNull null
                }

                val executableCjoHash = hashFile(executableArtifact.cjoPath, fileHashCache)
                val executableDynamicLibHash = hashFile(executableArtifact.dynamicLibPath, fileHashCache)
                val executableBchirHash = StableHash.sha256Of(
                    executableArtifact.dependenciesBchirPaths.map { hashFile(it, fileHashCache) },
                )
                val artifactSignature = artifact.signature ?: StableHash.sha256Of(
                    artifact.packageFqName.asString(),
                    exportedMacro.visibleName.asString(),
                    "visibleCjo=${artifact.cjoPath}",
                    "visibleCjoHash=$visibleCjoHash",
                    "visibleDylib=${artifact.dynamicLibPath}",
                    "visibleDylibHash=$visibleDynamicLibHash",
                    "executableTarget=${exportedMacro.executableFqName.asString()}",
                    "executableCjo=${executableArtifact.cjoPath}",
                    "executableCjoHash=$executableCjoHash",
                    "executableDylib=${executableArtifact.dynamicLibPath}",
                    "executableDylibHash=$executableDynamicLibHash",
                    "executableBchirHash=$executableBchirHash",
                    "executableAbi=${executableArtifact.abiVersion ?: artifact.abiVersion.orEmpty()}",
                    "ownerOrigin=${artifact.origin.name}",
                    "executableOrigin=${executableArtifact.origin.name}",
                    ALGORITHM_VERSION.toString(),
                )

                MacroDefinitionEntry(
                    packageFqName = artifact.packageFqName,
                    name = exportedMacro.visibleName,
                    executablePackageFqName = exportedMacro.executablePackageFqName,
                    executableName = exportedMacro.executableName,
                    source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                    libPath = executableArtifact.dynamicLibPath,
                    executorAbi = executableArtifact.abiVersion ?: artifact.abiVersion,
                    artifactSignature = artifactSignature,
                    cjoHash = visibleCjoHash,
                    dynamicLibHash = executableDynamicLibHash,
                    dependenciesBchirHash = executableBchirHash,
                    resolverAlgorithmVersion = ALGORITHM_VERSION,
                )
            }
        }

        return MacroArtifactResolverResult(definitions, diagnostics)
    }

    /**
     * 物理顶层宏声明和 `public import` 重导出的宏声明都应当计入 artifact surface。
     */
    private fun resolveExportedMacros(
        artifact: MacroArtifactPackage,
        header: CjoPackageHeader,
        exportedTopLevelNamesResolver: CjoExportedTopLevelNamesResolver?,
    ): List<ResolvedExportedMacro> {
        val exportedNames = exportedTopLevelNamesResolver?.resolve(artifact.packageFqName)
        return (exportedNames?.callableNames ?: header.topLevelCallableNames)
            .sortedBy { it.asString() }
            .map { visibleName ->
                val executableTarget = exportedNames?.callableTargets?.get(visibleName)
                ResolvedExportedMacro(
                    visibleName = visibleName,
                    executablePackageFqName = executableTarget?.packageFqName ?: artifact.packageFqName,
                    executableName = executableTarget?.name ?: visibleName,
                )
            }
    }

    private fun buildExportedTopLevelNamesResolver(
        packages: List<MacroArtifactPackage>,
        searchRoots: List<String>,
    ): CjoExportedTopLevelNamesResolver? {
        val resolvedRoots = (searchRoots + packages.mapNotNull { File(it.cjoPath).parentFile?.absolutePath })
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(::File)
            .filter(File::isDirectory)
            .map(File::getAbsolutePath)
            .distinct()
        if (resolvedRoots.isEmpty()) return null

        val joinedRoots = resolvedRoots.joinToString(File.pathSeparator)
        val manager = CjoManager(
            CjoSearchPath { envName ->
                when (envName) {
                    "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> joinedRoots
                    else -> null
                }
            },
        )
        return CjoExportedTopLevelNamesResolver(manager)
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

    private fun resolveExecutableArtifact(
        exportedMacro: ResolvedExportedMacro,
        ownerArtifact: MacroArtifactPackage,
        providedArtifactsByPackage: Map<FqName, MacroArtifactPackage>,
        artifactLocator: MacroArtifactLocator,
        searchRoots: List<String>,
        cache: MutableMap<FqName, MacroArtifactPackage?>,
    ): MacroArtifactPackage? {
        if (exportedMacro.executablePackageFqName == ownerArtifact.packageFqName) {
            return ownerArtifact
        }
        cache[exportedMacro.executablePackageFqName]?.let { return it }
        val provided = providedArtifactsByPackage[exportedMacro.executablePackageFqName]
        if (provided != null) {
            cache[exportedMacro.executablePackageFqName] = provided
            return provided
        }

        val located = artifactLocator.locate(
            packageDemands = setOf(exportedMacro.executablePackageFqName),
            searchRoots = searchRoots,
            explicitArtifacts = providedArtifactsByPackage.values.toList(),
        ).singleOrNull { it.packageFqName == exportedMacro.executablePackageFqName }
        cache[exportedMacro.executablePackageFqName] = located
        return located
    }

    private fun validateExecutableArtifact(
        ownerArtifact: MacroArtifactPackage,
        ownerMacro: ResolvedExportedMacro,
        executableArtifact: MacroArtifactPackage,
        expectedExecutorAbiVersion: String?,
    ): List<MacroConstructionDiagnostic> {
        if (executableArtifact.packageFqName == ownerArtifact.packageFqName) return emptyList()

        val diagnostics = mutableListOf<MacroConstructionDiagnostic>()
        val visibleMacroFqName = macroFqName(ownerArtifact.packageFqName, ownerMacro.visibleName)
        val executableMacroFqName = ownerMacro.executableFqName

        if (!File(executableArtifact.cjoPath).isFile) {
            diagnostics += ownerArtifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE,
                message = "Re-exported macro `${visibleMacroFqName.asString()}` resolves to `${executableMacroFqName.asString()}`, but the executable macro artifact `.cjo` was not found: ${executableArtifact.cjoPath}",
                artifactPath = executableArtifact.cjoPath,
                relatedTargets = listOf(executableMacroFqName),
            )
            return diagnostics
        }
        if (!File(executableArtifact.dynamicLibPath).isFile) {
            diagnostics += ownerArtifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_CANNOT_OPEN_LIB,
                message = "Re-exported macro `${visibleMacroFqName.asString()}` resolves to `${executableMacroFqName.asString()}`, but the executable macro dynamic library was not found: ${executableArtifact.dynamicLibPath}",
                macroLibraryPath = executableArtifact.dynamicLibPath,
                relatedTargets = listOf(executableMacroFqName),
            )
        }
        for (bchirPath in executableArtifact.dependenciesBchirPaths) {
            if (!File(bchirPath).isFile) {
                diagnostics += ownerArtifact.error(
                    kind = MacroConstructionDiagnostic.Kind.MACRO_CANNOT_FIND_DEPENDENCY_BCHIR,
                    message = "Re-exported macro `${visibleMacroFqName.asString()}` resolves to `${executableMacroFqName.asString()}`, but a dependency BCHIR of the executable macro artifact was not found: $bchirPath",
                    artifactPath = bchirPath,
                    relatedTargets = listOf(executableMacroFqName),
                )
            }
        }
        if (diagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }) {
            return diagnostics
        }

        val header = runCatching { readCjoHeader(File(executableArtifact.cjoPath)) }.getOrElse { error ->
            diagnostics += ownerArtifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                message = "Re-exported macro `${visibleMacroFqName.asString()}` resolves to `${executableMacroFqName.asString()}`, but the executable macro artifact `.cjo` cannot be read: ${error.message.orEmpty()}",
                artifactPath = executableArtifact.cjoPath,
                relatedTargets = listOf(executableMacroFqName),
            )
            return diagnostics
        }
        if (header.fullPkgName != ownerMacro.executablePackageFqName.asString()) {
            diagnostics += ownerArtifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE,
                message = "Re-exported macro `${visibleMacroFqName.asString()}` resolves to `${executableMacroFqName.asString()}`, but the executable artifact `${executableArtifact.cjoPath}` contains package `${header.fullPkgName}`.",
                artifactPath = executableArtifact.cjoPath,
                relatedTargets = listOf(executableMacroFqName),
            )
        }
        if (header.kind != PackageKind.Macro) {
            diagnostics += ownerArtifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                message = "Re-exported macro `${visibleMacroFqName.asString()}` resolves to `${executableMacroFqName.asString()}`, but package `${ownerMacro.executablePackageFqName.asString()}` is not a macro package artifact.",
                artifactPath = executableArtifact.cjoPath,
                relatedTargets = listOf(executableMacroFqName),
            )
        }
        val resolvedAbi = executableArtifact.abiVersion ?: ownerArtifact.abiVersion
        if (
            !resolvedAbi.isNullOrBlank() &&
            !expectedExecutorAbiVersion.isNullOrBlank() &&
            resolvedAbi != expectedExecutorAbiVersion
        ) {
            diagnostics += ownerArtifact.error(
                kind = MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION,
                message = "Re-exported macro `${visibleMacroFqName.asString()}` resolves to `${executableMacroFqName.asString()}`, but the executable artifact ABI `$resolvedAbi` does not match executor ABI `$expectedExecutorAbiVersion`.",
                artifactPath = executableArtifact.cjoPath,
                relatedTargets = listOf(executableMacroFqName),
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

    private fun hashFile(path: String, cache: MutableMap<String, String>): String =
        cache.getOrPut(path) {
            val file = File(path)
            MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes())
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
        }

    private fun MacroArtifactPackage.error(
        kind: MacroConstructionDiagnostic.Kind,
        message: String,
        artifactPath: String? = null,
        macroLibraryPath: String? = null,
        relatedTargets: List<FqName> = emptyList(),
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
            relatedTargets = relatedTargets,
        )
    }

    private fun macroFqName(packageFqName: FqName, name: Name): FqName =
        if (packageFqName.isRoot) FqName.topLevel(name) else packageFqName.child(name)

    private data class ResolvedExportedMacro(
        val visibleName: Name,
        val executablePackageFqName: FqName,
        val executableName: Name,
    ) {
        val executableFqName: FqName
            get() = if (executablePackageFqName.isRoot) {
                FqName.topLevel(executableName)
            } else {
                executablePackageFqName.child(executableName)
            }
    }
}
