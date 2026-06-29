package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cangjieSourceRoots
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.name.FqName
import java.io.File

/**
 * 宏展开前的 artifact 准备结果。
 *
 * 该结果只包含 resolver 校验后的宏定义和结构化诊断；调用方不得直接使用
 * orchestrator 返回的中间 artifact 绕过 locator/resolver。
 */
data class MacroExpansionArtifactPreparationResult(
    /**
     * resolver 验证通过后可供宏解析使用的定义列表。
     */
    val definitions: List<MacroDefinitionEntry> = emptyList(),
    /**
     * artifact 定位、即时编译和解析过程中产生的结构化诊断。
     */
    val diagnostics: List<MacroConstructionDiagnostic> = emptyList(),
    /**
     * 最终定位到并参与 resolver 校验的宏 artifact 包。
     */
    val locatedArtifacts: List<MacroArtifactPackage> = emptyList(),
) {
    /**
     * 当前准备结果是否包含错误级诊断。
     */
    val hasErrors: Boolean
        get() = diagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }
}

/**
 * 按真实宏调用 demand 准备外部宏包 artifact。
 *
 * 主链路固定为：
 * `pre-macro surfaces/imports -> production locator -> resolver`；
 * 缺失且配置允许时才调用独立 `cjc -p <root> --compile-macro -o <dir>`，
 * 调用成功后仍必须重新经过 locator/resolver。
 */
fun prepareMacroArtifactDefinitionsForExpansion(
    configuration: CompilerConfiguration,
    classifications: List<MacroDemandClassification>,
): MacroExpansionArtifactPreparationResult {
    val preResults = classifications.map { it.pre }
    val demandSurfacesByPackage = collectMacroExpansionPackageDemandSurfaces(classifications)
    val demandedMacroPackages = demandSurfacesByPackage.keys
    if (demandedMacroPackages.isEmpty()) return MacroExpansionArtifactPreparationResult()

    val artifactLocator = MacroArtifactLocator(configuration.macroSdkHome)
    val initialArtifacts = artifactLocator.locate(
        packageDemands = demandedMacroPackages,
        searchRoots = macroArtifactSearchRoots(configuration),
        explicitArtifacts = configuration.macroArtifactPackages,
    )
    val macroCompilation = compileRequiredMacroSourcePackages(
        configuration = configuration,
        preResults = preResults,
        locatedArtifacts = initialArtifacts,
        demandedMacroPackages = demandedMacroPackages,
    )
    val locatedArtifacts = artifactLocator.locate(
        packageDemands = demandedMacroPackages,
        searchRoots = macroArtifactSearchRoots(configuration) + macroCompilation.artifactSearchPaths,
        explicitArtifacts = configuration.macroArtifactPackages,
    )
    val artifactSearchRoots = macroArtifactSearchRoots(configuration) + macroCompilation.artifactSearchPaths
    val artifactResolution = MacroArtifactResolver().resolve(
        packages = locatedArtifacts,
        expectedExecutorAbiVersion = configuration.macroExecutorAbiVersion,
        searchRoots = artifactSearchRoots,
        sdkHome = configuration.macroSdkHome,
    )
    return MacroExpansionArtifactPreparationResult(
        definitions = artifactResolution.definitions,
        diagnostics = attachDemandSurfaceOrigins(
            diagnostics = macroCompilation.diagnostics + artifactResolution.diagnostics,
            demandSurfacesByPackage = demandSurfacesByPackage,
        ),
        locatedArtifacts = locatedArtifacts,
    )
}

/**
 * 将 artifact 级诊断复制到触发该包 demand 的宏表面上。
 */
private fun attachDemandSurfaceOrigins(
    diagnostics: List<MacroConstructionDiagnostic>,
    demandSurfacesByPackage: Map<FqName, List<MacroSurface>>,
): List<MacroConstructionDiagnostic> {
    if (diagnostics.isEmpty()) return emptyList()
    return diagnostics.flatMap { diagnostic ->
        if (diagnostic.originSurfaceId != null) return@flatMap listOf(diagnostic)
        val packageFqName = diagnostic.artifactPackage ?: return@flatMap listOf(diagnostic)
        val surfaces = demandSurfacesByPackage[packageFqName].orEmpty()
        if (surfaces.isEmpty()) {
            listOf(diagnostic)
        } else {
            surfaces.map { surface -> diagnostic.copy(originSurfaceId = surface.surfaceId) }
        }
    }
}

/**
 * 对缺失 artifact 的宏源码包发起按需编译。
 */
private fun compileRequiredMacroSourcePackages(
    configuration: CompilerConfiguration,
    preResults: List<PreMacroRawBuildResult>,
    locatedArtifacts: List<MacroArtifactPackage>,
    demandedMacroPackages: Set<FqName>,
): MacroPackageCompilationResult {
    if (demandedMacroPackages.isEmpty()) return MacroPackageCompilationResult()
    val missingDemandedPackages = demandedMacroPackages - locatedArtifacts.mapTo(linkedSetOf()) { it.packageFqName }
    if (missingDemandedPackages.isEmpty()) return MacroPackageCompilationResult()
    if (!configuration.macroExpansionDemandAutoCompilationEnabled) {
        return MacroPackageCompilationResult(
            diagnostics = missingDemandedPackages.map(::macroArtifactMissingWithAutoCompilationDisabled),
        )
    }
    val requests = selectMacroSourcePackageCompilationRequestsForExpansion(
        preResults = preResults,
        requests = macroSourcePackageCompilationRequestsForExpansion(configuration, preResults),
        suppliedArtifacts = locatedArtifacts,
    )
    val requestedPackages = requests.mapTo(linkedSetOf()) { it.packageFqName }
    val missingSourceRootDiagnostics = (missingDemandedPackages - requestedPackages)
        .map(::macroSourceRootMissingDiagnostic)
    if (requests.isEmpty()) {
        return MacroPackageCompilationResult(
            diagnostics = missingSourceRootDiagnostics,
        )
    }

    val orchestrator = configuration.macroPackageCompilationOrchestrator
        ?: return MacroPackageCompilationResult(
            diagnostics = missingSourceRootDiagnostics + unresolvedMacroPackageCompilationDiagnostics(requests),
        )

    val result = orchestrator.compileMacroPackages(
        requests = requests,
        context = MacroPackageCompilationContext(
            configuration = configuration,
            executorAbiVersion = configuration.macroExecutorAbiVersion,
            cacheContext = configuration.macroCompilationCacheContext,
        ),
    )
    return result.copy(diagnostics = missingSourceRootDiagnostics + result.diagnostics)
}

/**
 * 计算本次宏 artifact 定位应搜索的根目录列表。
 */
private fun macroArtifactSearchRoots(configuration: CompilerConfiguration): List<String> =
    (configuration.classpathRoots.map { File(it.path).absolutePath } +
        configuration.macroSourcePackageCompilationRequests.flatMap { request ->
            request.importPaths + request.classpath
        })
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()

/**
 * 合并显式配置和同项目发现得到的宏源码包编译请求。
 */
private fun macroSourcePackageCompilationRequestsForExpansion(
    configuration: CompilerConfiguration,
    preResults: List<PreMacroRawBuildResult>,
): List<MacroSourcePackageCompilationRequest> {
    val configuredRequests = configuration.macroSourcePackageCompilationRequests
    val configuredPackages = configuredRequests.mapTo(linkedSetOf()) { it.packageFqName }
    val discoveredRequests = discoverSameProjectMacroSourcePackageCompilationRequests(configuration, preResults)
        .filter { it.packageFqName !in configuredPackages }
    return configuredRequests + discoveredRequests
}

/**
 * 从本次同项目源码中发现可即时编译的 `macro package` 源根。
 *
 * 该发现只产生候选 [MacroSourcePackageCompilationRequest]；是否真的调用
 * `cjc -p <root> --compile-macro` 仍由真实宏调用 demand、locator miss 和开关共同决定。
 */
private fun discoverSameProjectMacroSourcePackageCompilationRequests(
    configuration: CompilerConfiguration,
    preResults: List<PreMacroRawBuildResult>,
): List<MacroSourcePackageCompilationRequest> {
    val sourceRoots = configuration.cangjieSourceRoots
        .map { File(it.path).absoluteFile.normalize() }
        .filter(File::exists)
    if (sourceRoots.isEmpty()) return emptyList()

    val packageRoots = linkedMapOf<FqName, MutableSet<File>>()
    for (pre in preResults) {
        for (preFile in pre.files) {
            if (!preFile.isMacroPackage) continue
            val sourcePath = preFile.cfirFile.sourceFile?.path ?: continue
            val sourceFile = File(sourcePath).absoluteFile.normalize()
            val root = sourceRoots
                .filter { sourceRoot -> sourceFile.isUnderOrEqual(sourceRoot) }
                .maxByOrNull { it.path.length }
                ?.let { sourceRoot -> if (sourceRoot.isFile) sourceRoot.parentFile else sourceRoot }
                ?: continue
            packageRoots
                .getOrPut(preFile.cfirFile.packageDirective.packageFqName) { linkedSetOf() }
                .add(root)
        }
    }

    val classpath = configuration.classpathRoots.map { File(it.path).absolutePath }.distinct()
    return packageRoots.map { (packageFqName, roots) ->
        MacroSourcePackageCompilationRequest(
            packageFqName = packageFqName,
            sourceRoots = roots.map { it.path }.distinct(),
            importPaths = classpath,
            classpath = classpath,
            compileInvocationId = "macro-source-${packageFqName.asString()}",
        )
    }
}

/**
 * 判断当前文件是否位于指定根目录之内或等于根目录本身。
 */
private fun File.isUnderOrEqual(root: File): Boolean {
    val thisPath = canonicalFile.toPath()
    val rootPath = root.canonicalFile.toPath()
    return thisPath == rootPath || thisPath.startsWith(rootPath)
}

/**
 * 构造关闭按需编译时缺失宏 artifact 的诊断。
 */
private fun macroArtifactMissingWithAutoCompilationDisabled(packageFqName: FqName): MacroConstructionDiagnostic =
    MacroConstructionDiagnostic(
        severity = MacroConstructionDiagnostic.Severity.ERROR,
        message = "Macro package `${packageFqName.asString()}` requires compiled `.cjo` and dynamic library artifacts, but expansion-demand macro package compilation is disabled.",
        kind = MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED,
        artifactPackage = packageFqName,
        diagnosticOrigin = MacroConstructionDiagnostic.Origin.ORCHESTRATION,
    )

/**
 * 构造找不到同项目宏源码根时的诊断。
 */
private fun macroSourceRootMissingDiagnostic(packageFqName: FqName): MacroConstructionDiagnostic =
    MacroConstructionDiagnostic(
        severity = MacroConstructionDiagnostic.Severity.ERROR,
        message = "Macro package `${packageFqName.asString()}` requires compiled `.cjo` and dynamic library artifacts, but no same-project macro source root is configured for `cjc -p <root> --compile-macro`.",
        kind = MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED,
        artifactPackage = packageFqName,
        diagnosticOrigin = MacroConstructionDiagnostic.Origin.ORCHESTRATION,
    )
