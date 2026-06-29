package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.source.CjSourceElement
import PackageFormat.Package
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.outputStream

/**
 * 待独立编译的源码宏包请求。
 *
 * 前端只声明 orchestration 合同，不直接创建进程或递归调用编译器。
 * CLI / Gradle / IDE build graph 必须在该接口实现中执行 `--compile-macro`
 * invocation，并把成功产物转成 [MacroArtifactPackage]。
 */
data class MacroSourcePackageCompilationRequest(
    /**
     * 需要独立编译的宏包全限定名。
     */
    val packageFqName: FqName,
    /**
     * 传给 `cjc -p` 的源码根候选列表。
     */
    val sourceRoots: List<String>,
    /**
     * 编译宏包时传入的 import path。
     */
    val importPaths: List<String> = emptyList(),
    /**
     * 编译宏包时暴露给 `CANGJIE_LIBRARY` 的 classpath。
     */
    val classpath: List<String> = emptyList(),
    /**
     * 传给外部 cjc invocation 的额外编译选项。
     */
    val compilerOptions: List<String> = emptyList(),
    /**
     * 宏包编译输出目录。
     */
    val outputDirectory: String? = null,
    /**
     * 外部构建系统为本次宏包编译分配的调用标识。
     */
    val compileInvocationId: String? = null,
    /**
     * 外部编译原始诊断输出引用。
     */
    val sourceDiagnosticsRef: String? = null,
    /**
     * 触发该请求的源位置。
     */
    val originSource: CjSourceElement? = null,
)

/**
 * 外部 macro 编译调度的 cache 环境。
 *
 * 这些字段由 build/CLI 层提供；前端把它们合入 macro expansion cache key，
 * 避免 PATH/动态库加载环境、高层 target platform 身份、并行/debug flag 变化时复用旧展开结果。
 */
data class MacroCompilationCacheContext(
    /**
     * 编译选项指纹。
     */
    val compilerOptionsFingerprint: String = "",
    /**
     * 调试开关指纹。
     */
    val debugFlagsFingerprint: String = "",
    /**
     * 并行相关开关指纹。
     */
    val parallelFlagsFingerprint: String = "",
    /**
     * 宏包编译目标平台。
     */
    val targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
    /**
     * 运行时加载环境指纹。
     */
    val runtimeLoaderEnvironmentFingerprint: String = "",
)

/**
 * 宏包编译 orchestrator 调用上下文。
 */
data class MacroPackageCompilationContext(
    /**
     * 当前前端编译配置。
     */
    val configuration: CompilerConfiguration,
    /**
     * 宏执行器 ABI 版本。
     */
    val executorAbiVersion: String,
    /**
     * 影响宏展开缓存键的编译环境上下文。
     */
    val cacheContext: MacroCompilationCacheContext,
)

/**
 * 宏包编译 orchestrator 输出结果。
 */
data class MacroPackageCompilationResult(
    /**
     * 编译成功后产出的 artifact 包。
     */
    val artifactPackages: List<MacroArtifactPackage> = emptyList(),
    /**
     * 可继续用于 artifact locator 的输出搜索路径。
     */
    val artifactSearchPaths: List<String> = emptyList(),
    /**
     * 编译调度过程中产生的结构化诊断。
     */
    val diagnostics: List<MacroConstructionDiagnostic> = emptyList(),
) {
    /**
     * 当前结果是否包含错误级诊断。
     */
    val hasErrors: Boolean
        get() = diagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }
}

/**
 * 外部宏包编译调度接口。
 */
fun interface MacroPackageCompilationOrchestrator {
    /**
     * 编译一批宏源码包并返回可解析 artifact。
     */
    fun compileMacroPackages(
        requests: List<MacroSourcePackageCompilationRequest>,
        context: MacroPackageCompilationContext,
    ): MacroPackageCompilationResult
}

/**
 * 从本次宏展开真实需求中筛选需要编译的宏源码包。
 *
 * [requests] 表示 middleware/build graph 已发现的可编译宏源码包候选；
 * 本函数只在 `pre` 中存在实际 macro surface，且该 surface 通过全限定调用或
 * import 依赖某个候选包，同时该包尚未有已编译 artifact 时，才返回对应 request。
 */
internal fun selectMacroSourcePackageCompilationRequestsForExpansion(
    preResults: List<PreMacroRawBuildResult>,
    requests: List<MacroSourcePackageCompilationRequest>,
    suppliedArtifacts: List<MacroArtifactPackage>,
): List<MacroSourcePackageCompilationRequest> {
    if (requests.isEmpty() || preResults.none { it.allSurfaces.isNotEmpty() }) return emptyList()

    val demandedPackages = collectMacroExpansionPackageDemands(preResults)
    if (demandedPackages.isEmpty()) return emptyList()

    val alreadyCompiledPackages = suppliedArtifacts.mapTo(linkedSetOf()) { it.packageFqName }
    return requests.filter { request ->
        request.packageFqName in demandedPackages && request.packageFqName !in alreadyCompiledPackages
    }
}

/**
 * 从 pre-macro 结果中收集真实触发宏展开的包需求。
 */
internal fun collectMacroExpansionPackageDemands(preResults: List<PreMacroRawBuildResult>): Set<FqName> {
    return collectMacroExpansionPackageDemandSurfaces(preResults.map { MacroDemandClassification.create(it) }).keys
}

/**
 * 从 pre-macro 结果中收集每个宏包 demand 对应的宏表面。
 */
internal fun collectMacroExpansionPackageDemandSurfacesFromPreResults(
    preResults: List<PreMacroRawBuildResult>,
): Map<FqName, List<MacroSurface>> {
    val demandedPackages = linkedSetOf<FqName>()
    val demandedSurfaces = linkedMapOf<FqName, MutableList<MacroSurface>>()

    fun addDemand(packageFqName: FqName, surface: MacroSurface) {
        demandedPackages += packageFqName
        demandedSurfaces.getOrPut(packageFqName) { mutableListOf() } += surface
    }

    for (pre in preResults) {
        for (preFile in pre.files) {
            if (preFile.isMacroPackage || preFile.cfirFile.declarations.containsMacroDeclaration()) continue
            val callableSurfaces = preFile.surfaces.filter { it.isMacroExpansionDemandSurface() }
            for (surface in callableSurfaces) {
                val packageFqName = surface.qualifiedName
                    ?.parent()
                    ?.takeUnless {
                        it.isRoot ||
                                it == surface.scopeContext.packageFqName ||
                                it == preFile.cfirFile.packageDirective.packageFqName
                    }
                if (packageFqName != null) {
                    addDemand(packageFqName, surface)
                }
            }

            val macroSurfacesByName = callableSurfaces
                .mapNotNull { surface -> surface.qualifiedName?.shortName()?.let { name -> name to surface } }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            if (macroSurfacesByName.isEmpty()) continue

            for (import in preFile.cfirFile.imports) {
                val importedFqName = import.importedFqName ?: continue
                val packageFqName = if (import.isAllUnder) importedFqName else importedFqName.parent()
                if (packageFqName.isRoot) continue

                val importCanBindMacroSurface = import.isAllUnder ||
                        (import.aliasName ?: importedFqName.shortName()) in macroSurfacesByName.keys
                if (importCanBindMacroSurface) {
                    val matchedSurfaces = if (import.isAllUnder) {
                        callableSurfaces
                    } else {
                        macroSurfacesByName[import.aliasName ?: importedFqName.shortName()].orEmpty()
                    }
                    matchedSurfaces.forEach { surface -> addDemand(packageFqName, surface) }
                }
            }
        }
    }
    return demandedPackages.associateWith { packageFqName ->
        demandedSurfaces[packageFqName].orEmpty().distinctBy { it.surfaceId }
    }
}

/**
 * 从已分类的宏需求快照中收集每个外部包对应的宏表面。
 */
internal fun collectMacroExpansionPackageDemandSurfaces(
    classifications: List<MacroDemandClassification>,
): Map<FqName, List<MacroSurface>> {
    return classifications
        .flatMap { classification -> classification.preArtifactSnapshot.externalPackageDemandSurfaces.entries }
        .groupBy(keySelector = { it.key }, valueTransform = { it.value })
        .mapValues { (_, grouped) -> grouped.flatten().distinctBy { it.surfaceId } }
}

/**
 * `macro package` 内的 `public macro` 签名 surface 是编译 macro artifact 的输入，
 * 不是使用方触发展开的 demand。
 */
private fun MacroSurface.isMacroDefinitionSignatureSurface(): Boolean {
    val carrier = replaceHandle.carrier
    if (carrier is CfirMacroDeclaration) return true
    return carrier is CfirValueParameter &&
            carrier.containingDeclarationSymbol is CfirMacroDeclarationSymbol
}

/**
 * 判断宏表面是否是真实使用方展开 demand。
 */
private fun MacroSurface.isMacroExpansionDemandSurface(): Boolean {
    if (isMacroDefinitionSignatureSurface()) return false
    return capturedRawSyntax?.trimStart()?.startsWith("@") == true
}

/**
 * 判断声明列表中是否包含宏声明。
 */
private fun List<CfirDeclaration>.containsMacroDeclaration(): Boolean {
    return any { declaration ->
        when (declaration) {
            is CfirMacroDeclaration -> true
            is CfirClassLikeDeclaration -> declaration.nestedDeclarationsContainMacro()
            else -> false
        }
    }
}

/**
 * 判断类状声明的嵌套声明中是否包含宏声明。
 */
private fun CfirClassLikeDeclaration.nestedDeclarationsContainMacro(): Boolean {
    val nested = when (this) {
        is CfirClass -> declarations
        is CfirInterface -> declarations
        is CfirStruct -> declarations
        is CfirEnum -> declarations
        else -> emptyList()
    }
    return nested.containsMacroDeclaration()
}

/**
 * 基于外部 `cjc --compile-macro` 的宏包编译 orchestrator。
 *
 * 这是 Phase 2 的“独立 invocation 调度”实现：
 * - 前端仍然只消费 [MacroArtifactPackage] 和结构化诊断；
 * - 实际宏包源码编译通过外部 `cjc` 进程完成；
 * - 成功后发现 `.cjo + 动态库 + 依赖 BCHIR`，投影成 artifact；
 * - 失败时返回 `MACRO_DEPENDENCY_COMPILE_FAILED`，并保留 `compileInvocationId`
 *   与 `sourceDiagnosticsRef` 供使用方诊断引用。
 */
class ExternalCjcMacroPackageCompilationOrchestrator(
    /**
     * 外部 cjc 可执行文件解析器。
     */
    private val executableResolver: MacroCompilerExecutableResolver = DefaultMacroCompilerExecutableResolver,
    /**
     * 外部 cjc 命令执行器。
     */
    private val commandRunner: MacroCompilerCommandRunner = ProcessMacroCompilerCommandRunner,
) : MacroPackageCompilationOrchestrator {
    /**
     * 调用外部 cjc 编译宏源码包并收集产物。
     */
    override fun compileMacroPackages(
        requests: List<MacroSourcePackageCompilationRequest>,
        context: MacroPackageCompilationContext,
    ): MacroPackageCompilationResult {
        if (requests.isEmpty()) return MacroPackageCompilationResult()

        val cjcPath = runCatching { resolveCompilerExecutable(context) }.getOrElse { error ->
            return MacroPackageCompilationResult(
                diagnostics = requests.map { request ->
                    request.compilationError(
                        message = "Cannot locate `cjc` for macro package `${request.packageFqName.asString()}`: ${error.message.orEmpty()}",
                    )
                },
            )
        }

        val artifacts = mutableListOf<MacroArtifactPackage>()
        val artifactSearchPaths = mutableListOf<String>()
        val diagnostics = mutableListOf<MacroConstructionDiagnostic>()

        for (request in requests) {
            val sourceRoots = request.sourceRoots
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
            if (sourceRoots.isEmpty()) {
                diagnostics += request.compilationError("Macro package `${request.packageFqName.asString()}` has no source roots to compile.")
                continue
            }
            if (sourceRoots.size != 1) {
                diagnostics += request.compilationError(
                    "Macro package `${request.packageFqName.asString()}` must map to exactly one package source root for `cjc -p <dir> --compile-macro`; actual roots: ${sourceRoots.joinToString()}",
                )
                continue
            }

            val sourceRoot = Path.of(sourceRoots.single())
            val outputDirectory = request.outputDirectory
                ?.takeIf(String::isNotBlank)
                ?.let(Path::of)
                ?: defaultOutputDirectoryFor(request, sourceRoot)
            Files.createDirectories(outputDirectory)

            val command = buildCommand(
                cjcPath = cjcPath,
                sourceRoot = sourceRoot,
                request = request,
                outputDirectory = outputDirectory,
            )
            val execution = runCatching { commandRunner.run(command) }.getOrElse { error ->
                diagnostics += request.compilationError(
                    message = "Macro package `${request.packageFqName.asString()}` invocation failed before completion: ${error.message.orEmpty()}",
                    sourceDiagnosticsRef = persistDiagnosticsOutput(
                        request,
                        outputDirectory,
                        "",
                        error.stackTraceToString()
                    ),
                )
                continue
            }

            val diagnosticsRef = request.sourceDiagnosticsRef
                ?: persistDiagnosticsOutput(request, outputDirectory, execution.stdout, execution.stderr)

            if (execution.exitCode != 0) {
                diagnostics += request.compilationError(
                    message = buildString {
                        append("Macro package `${request.packageFqName.asString()}` compilation failed")
                        append(" (exit code ${execution.exitCode})")
                        execution.stderr.lineSequence().firstOrNull { it.isNotBlank() }?.let { firstError ->
                            append(": ")
                            append(firstError)
                        }
                    },
                    sourceDiagnosticsRef = diagnosticsRef,
                )
                continue
            }

            val resolvedArtifact = resolveCompiledArtifact(
                request = request,
                outputDirectory = outputDirectory,
                executorAbiVersion = context.executorAbiVersion,
                sourceDiagnosticsRef = diagnosticsRef,
            )
            if (resolvedArtifact == null) {
                diagnostics += request.compilationError(
                    message = "Macro package `${request.packageFqName.asString()}` compilation succeeded but required artifact files were not found under `${outputDirectory.absolutePathString()}`.",
                    sourceDiagnosticsRef = diagnosticsRef,
                )
                continue
            }

            artifacts += resolvedArtifact
            artifactSearchPaths += outputDirectory.absolutePathString()
        }

        return MacroPackageCompilationResult(
            artifactPackages = artifacts,
            artifactSearchPaths = artifactSearchPaths.distinct(),
            diagnostics = diagnostics,
        )
    }

    /**
     * 解析当前环境中可用的 cjc 可执行文件。
     */
    private fun resolveCompilerExecutable(context: MacroPackageCompilationContext): Path {
        if (executableResolver === DefaultMacroCompilerExecutableResolver) {
            return DefaultMacroCompilerExecutableResolver.resolve(context.configuration.macroSdkHome)
        }
        return executableResolver.resolve()
    }

    /**
     * 构造一次 `cjc -p <root> --compile-macro` 命令。
     */
    private fun buildCommand(
        cjcPath: Path,
        sourceRoot: Path,
        request: MacroSourcePackageCompilationRequest,
        outputDirectory: Path,
    ): MacroCompilerCommand {
        val arguments = buildList {
            add(cjcPath.absolutePathString())
            add("-p")
            add(sourceRoot.absolutePathString())
            add("--compile-macro")
            add("-o")
            add(outputDirectory.absolutePathString())
            request.importPaths
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach { importPath ->
                    add("--import-path")
                    add(importPath)
                }
            addAll(request.compilerOptions)
        }
        val environment = buildMap {
            val classpath = request.classpath
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
            if (classpath.isNotEmpty()) {
                put("CANGJIE_LIBRARY", classpath.joinToString(File.pathSeparator))
            }
        }
        return MacroCompilerCommand(
            arguments = arguments,
            workingDirectory = outputDirectory.toFile(),
            environment = environment,
        )
    }

    /**
     * 从外部编译输出目录中解析出宏 artifact 包。
     */
    private fun resolveCompiledArtifact(
        request: MacroSourcePackageCompilationRequest,
        outputDirectory: Path,
        executorAbiVersion: String,
        sourceDiagnosticsRef: String?,
    ): MacroArtifactPackage? {
        val cjoPath = findCjoForPackage(outputDirectory, request.packageFqName) ?: return null
        val dynamicLibPath = findDynamicLibraryForPackage(outputDirectory, request.packageFqName) ?: return null
        val dependencyBchirPaths = discoverDependencyBchirPaths(request)

        return MacroArtifactPackage(
            packageFqName = request.packageFqName,
            kind = MacroArtifactPackage.Kind.MACRO,
            cjoPath = cjoPath.absolutePathString(),
            dynamicLibPath = dynamicLibPath.absolutePathString(),
            dependenciesBchirPaths = dependencyBchirPaths,
            abiVersion = executorAbiVersion,
            origin = MacroArtifactPackage.Origin.ORCHESTRATION,
            compileInvocationId = request.compileInvocationId,
            sourceDiagnosticsRef = sourceDiagnosticsRef,
        )
    }

    /**
     * 在输出目录中查找并校验目标包的 `.cjo` 文件。
     */
    private fun findCjoForPackage(outputDirectory: Path, packageFqName: FqName): Path? {
        val cjoName = "${toCjoFileName(packageFqName)}.cjo"
        val firstSegment = packageFqName.firstSegment()?.asString()
        return listOfNotNull(
            firstSegment?.let { outputDirectory.resolve(it).resolve(cjoName) },
            outputDirectory.resolve(cjoName),
        ).firstOrNull { path ->
            path.toFile().isFile &&
                    runCatching {
                        val header = readCjoHeader(path)
                        header.fullPkgName == packageFqName.asString() &&
                                header.kind == PackageFormat.PackageKind.Macro
                    }.getOrDefault(false)
        }
    }

    /**
     * 在输出目录中查找目标包的动态库。
     */
    private fun findDynamicLibraryForPackage(outputDirectory: Path, packageFqName: FqName): Path? {
        val libraryName = "lib-macro_${toCjoFileName(packageFqName)}.${dynamicLibraryExtension()}"
        val firstSegment = packageFqName.firstSegment()?.asString()
        return listOfNotNull(
            firstSegment?.let { outputDirectory.resolve(it).resolve(libraryName) },
            outputDirectory.resolve(libraryName),
        ).firstOrNull { it.toFile().isFile }
    }

    /**
     * 从请求的 import/classpath 根中发现依赖 BCHIR 文件。
     */
    private fun discoverDependencyBchirPaths(request: MacroSourcePackageCompilationRequest): List<String> {
        return (request.importPaths + request.classpath)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .flatMap { root ->
                val path = Path.of(root)
                if (!path.exists() || !path.isDirectory()) return@flatMap emptyList()
                path.walkTopDown()
                    .filter { it.isFile && it.extension == "bchir" }
                    .map { it.absolutePath }
                    .toList()
            }
            .distinct()
    }

    /**
     * 将外部 cjc 的 stdout/stderr 持久化为诊断引用文件。
     */
    private fun persistDiagnosticsOutput(
        request: MacroSourcePackageCompilationRequest,
        outputDirectory: Path,
        stdout: String,
        stderr: String,
    ): String? {
        if (stdout.isBlank() && stderr.isBlank()) return null
        val diagnosticsFile = outputDirectory.resolve(
            ".macro-compile-${request.packageFqName.asString().replace('.', '_')}.log",
        )
        val payload = buildString {
            appendLine("[stdout]")
            append(stdout)
            if (stdout.isNotBlank() && !stdout.endsWith(System.lineSeparator())) {
                appendLine()
            }
            appendLine("[stderr]")
            append(stderr)
        }
        diagnosticsFile.outputStream().use { stream ->
            stream.write(payload.toByteArray(StandardCharsets.UTF_8))
        }
        return diagnosticsFile.absolutePathString()
    }

    /**
     * 根据源码根和包名计算默认宏编译输出目录。
     */
    private fun defaultOutputDirectoryFor(
        request: MacroSourcePackageCompilationRequest,
        sourceRoot: Path,
    ): Path {
        val packageSegment = request.packageFqName.asString().replace('.', '_')
        return sourceRoot.parent?.resolve(".macro-build")?.resolve(packageSegment)
            ?: Files.createTempDirectory("macro-build-$packageSegment")
    }

    /**
     * 以文件序列形式遍历路径树。
     */
    private fun Path.walkTopDown(): Sequence<File> {
        val root = toFile()
        if (!root.exists()) return emptySequence()
        return root.walkTopDown()
    }

    /**
     * 读取 `.cjo` 文件头部元数据。
     */
    private fun readCjoHeader(path: Path): CjoPackageHeader {
        val bytes = Files.readAllBytes(path)
        val buffer = ByteBuffer.wrap(bytes)
        return CjoPackageHeader.fromPackage(Package.getRootAsPackage(buffer))
    }
}

/**
 * cjc 可执行文件解析器。
 */
fun interface MacroCompilerExecutableResolver {
    /**
     * 返回可执行 cjc 路径。
     */
    fun resolve(): Path
}

/**
 * 外部宏编译命令模型。
 */
data class MacroCompilerCommand(
    /**
     * 命令行参数，第一项是可执行文件路径。
     */
    val arguments: List<String>,
    /**
     * 进程工作目录。
     */
    val workingDirectory: File,
    /**
     * 附加到进程环境变量中的键值。
     */
    val environment: Map<String, String>,
)

/**
 * 外部宏编译命令执行结果。
 */
data class MacroCompilerCommandResult(
    /**
     * 进程退出码。
     */
    val exitCode: Int,
    /**
     * 标准输出文本。
     */
    val stdout: String,
    /**
     * 标准错误文本。
     */
    val stderr: String,
)

/**
 * 外部宏编译命令执行器。
 */
fun interface MacroCompilerCommandRunner {
    /**
     * 执行给定命令并返回退出结果。
     */
    fun run(command: MacroCompilerCommand): MacroCompilerCommandResult
}

/**
 * 默认 cjc 可执行文件解析器。
 */
object DefaultMacroCompilerExecutableResolver : MacroCompilerExecutableResolver {
    /**
     * 使用默认 SDK 配置解析 cjc。
     */
    override fun resolve(): Path = resolve(DEFAULT_MACRO_SDK_HOME)

    /**
     * 使用指定 SDK 根解析 cjc。
     */
    fun resolve(sdkHome: String): Path {
        return resolve(
            sdkHome = sdkHome,
            cangjieHome = System.getenv("CANGJIE_HOME"),
            cjcHome = System.getProperty("cjc.home"),
            pathValue = System.getenv("PATH"),
            userHome = Path.of(System.getProperty("user.home")),
        )
    }

    /**
     * 按 SDK、环境变量、系统属性、PATH 和用户目录兜底顺序解析 cjc。
     */
    internal fun resolve(
        sdkHome: String,
        cangjieHome: String?,
        cjcHome: String?,
        pathValue: String?,
        userHome: Path,
    ): Path {
        val executableName = if (isWindows()) "cjc.exe" else "cjc"

        sdkHome.takeIf(String::isNotBlank)?.let { home ->
            Path.of(home, "bin", executableName).takeIf(Path::exists)?.let { return it }
        }
        cangjieHome?.takeIf(String::isNotBlank)?.let { home ->
            Path.of(home, "bin", executableName).takeIf(Path::exists)?.let { return it }
        }
        cjcHome?.takeIf(String::isNotBlank)?.let { home ->
            Path.of(home, "bin", executableName).takeIf(Path::exists)?.let { return it }
        }
        findOnPath(executableName, pathValue.orEmpty())?.let { return it }

        userHome.resolve("sdk").toFile().listFiles()
            ?.filter { it.name.startsWith("cangjie-sdk-") }
            ?.sortedByDescending { it.name }
            ?.firstOrNull()
            ?.toPath()
            ?.resolve("cangjie")
            ?.resolve("bin")
            ?.resolve(executableName)
            ?.takeIf(Path::exists)
            ?.let { return it }

        error("Cannot find `$executableName`. Set CANGJIE_HOME or cjc.home, or place cjc on PATH.")
    }

    /**
     * 在 PATH 环境变量中查找可执行文件。
     */
    private fun findOnPath(executableName: String, pathValue: String): Path? {
        if (pathValue.isBlank()) return null
        return pathValue.split(File.pathSeparatorChar)
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { Path.of(it).resolve(executableName) }
            .firstOrNull(Path::exists)
    }

    /**
     * 当前系统是否为 Windows。
     */
    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}

/**
 * 基于 [ProcessBuilder] 的外部 cjc 命令执行器。
 */
object ProcessMacroCompilerCommandRunner : MacroCompilerCommandRunner {
    /**
     * 启动外部进程并收集 stdout/stderr/exitCode。
     */
    override fun run(command: MacroCompilerCommand): MacroCompilerCommandResult {
        val process = ProcessBuilder(command.arguments)
            .directory(command.workingDirectory)
            .redirectErrorStream(false)
            .apply {
                environment().putAll(command.environment)
            }
            .start()

        val stdout = process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        val stderr = process.errorStream.bufferedReader(StandardCharsets.UTF_8).readText()
        val exitCode = process.waitFor()
        return MacroCompilerCommandResult(
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
        )
    }
}

/**
 * 构造缺失宏包编译 orchestrator 时的诊断列表。
 */
internal fun unresolvedMacroPackageCompilationDiagnostics(
    requests: List<MacroSourcePackageCompilationRequest>,
): List<MacroConstructionDiagnostic> = requests.map { request ->
    MacroConstructionDiagnostic(
        severity = MacroConstructionDiagnostic.Severity.ERROR,
        message = "Macro package `${request.packageFqName.asString()}` requires `--compile-macro` artifact, but no macro package compilation orchestrator is configured.",
        originSource = request.originSource,
        kind = MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED,
        artifactPackage = request.packageFqName,
        diagnosticOrigin = MacroConstructionDiagnostic.Origin.ORCHESTRATION,
        compileInvocationId = request.compileInvocationId,
        sourceDiagnosticsRef = request.sourceDiagnosticsRef,
    )
}

/**
 * 根据宏包编译请求构造编译失败诊断。
 */
private fun MacroSourcePackageCompilationRequest.compilationError(
    message: String,
    sourceDiagnosticsRef: String? = this.sourceDiagnosticsRef,
): MacroConstructionDiagnostic {
    return MacroConstructionDiagnostic(
        severity = MacroConstructionDiagnostic.Severity.ERROR,
        message = message,
        originSource = originSource,
        kind = MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED,
        artifactPackage = packageFqName,
        diagnosticOrigin = MacroConstructionDiagnostic.Origin.ORCHESTRATION,
        compileInvocationId = compileInvocationId,
        sourceDiagnosticsRef = sourceDiagnosticsRef,
    )
}
