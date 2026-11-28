package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.pipeline.runPlatformCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.entrypoint.session.createDefaultCfirSessionFactoryContext
import org.cangnova.cangjie.cfir.entrypoint.session.createCfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.pipeline.*
import org.cangnova.cangjie.cfir.resolve.providers.macro.*
import org.cangnova.cangjie.cfir.serialization.cjo.CfirCjoPackageMetadataProducer
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportSourceSnapshots
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportOptions
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.cfir.entrypoint.configuration.CjmpCommonPartInputResolution
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartInputResolution
import org.cangnova.cangjie.frontend.environment.VfsBasedProjectEnvironment
import org.cangnova.cangjie.frontend.environment.findFileByPath
import org.cangnova.cangjie.frontend.environment.forAllFiles
import org.cangnova.cangjie.frontend.sources.CollectedCjSources
import org.cangnova.cangjie.frontend.sources.GroupedCjSources
import org.cangnova.cangjie.frontend.sources.allFiles
import org.cangnova.cangjie.frontend.sources.acceptsCangjieSource
import org.cangnova.cangjie.frontend.sources.cangjieSourceKind
import org.cangnova.cangjie.frontend.sources.collectCjSources
import org.cangnova.cangjie.messages.CompilerMessageLocationWithRange
import org.cangnova.cangjie.messages.CompilerMessageLocation
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.cfir.pipeline.reportParsingDiagnostics
import org.cangnova.cangjie.cfir.pipeline.collectParsingDiagnosticPublication
import org.cangnova.cangjie.config.parserSettings
import org.cangnova.cangjie.config.parsingDiagnosticSettings
import org.cangnova.cangjie.parsing.toCangJieParsingErrorListener
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.text
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * CFIR 前端管线阶段。
 *
 * 该阶段负责创建 VFS 环境、收集源码、构建 CFIR session、执行宏构造前 raw CFIR 构建，
 * 再完成宏构造、resolve 与 check，并产出所有模块的前端输出。
 */
object CfirFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, DefaultCfirFrontendPipelineArtifact>(
    name = "CfirFrontendPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector),
) {
    /**
     * 执行 CFIR 前端阶段。
     */
    override fun executePhase(input: ConfigurationPipelineArtifact): DefaultCfirFrontendPipelineArtifact? {
        val (configuration, rootDisposable) = input
        val initialDiagnosticCount = configuration.diagnosticsCollector.diagnostics.size
        val commonPartInputs = when (val resolution = configuration.cjmpCommonPartInputResolution) {
            is CjmpCommonPartInputResolution.Valid -> resolution.inputs
            is CjmpCommonPartInputResolution.Invalid -> {
                configuration.messageCollector.report(CompilerMessageSeverity.ERROR, resolution.message)
                return null
            }
        }
        configuration.initializeCfirFrontendMacroCompilationConfiguration()

        val (environment, sourcesProvider) = createEnvironmentAndSources(configuration, rootDisposable) ?: return null
        val sources = sourcesProvider()

        if (sources.allSources.isEmpty()) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "No source files",
            )
            return null
        }

        val rootModuleName = Name.identifier(configuration.moduleName ?: "main")
        val factory = CfirDefaultSessionFactory()
        val sessionFactoryContext = createDefaultCfirSessionFactoryContext(configuration, commonPartInputs)
        val extensionRegistrars = emptyList<CfirExtensionRegistrar>()
        configuration.installDefaultMacroFragmentParserFactory(environment.project)

        // Baseline 第 1 节"主流程"：
        //   pre → MacroConstructionService.expand → recordExpandedRawFilesOnce → resolve & check
        //
        // 当前 batch 仍以 STRICT 模式驱动 CLI：构造失败立刻终止该 module 的 resolve。
        val sessionsWithSources = buildSessions(
            configuration = configuration,
            rootModuleName = rootModuleName,
            groupedSources = sources.groupedSources,
            classpathRoots = sources.classpathRoots,
            factory = factory,
            sessionFactoryContext = sessionFactoryContext,
            commonPartInputs = commonPartInputs,
            extensionRegistrars = extensionRegistrars,
        )

        val sessionPreResults = sessionsWithSources.map { sessionWithSources ->
            SessionPreMacroResult(
                session = sessionWithSources.session,
                librarySession = sessionWithSources.librarySession,
                pre = buildPreMacroFromSources(
                    session = sessionWithSources.session,
                    sources = sessionWithSources.files,
                    environment = environment,
                    useLightTree = configuration.useLightTree,
                    parserSettings = configuration.parserSettings,
                ).pruneConditionalCompilation(),
            )
        }
        val parserFiles = sessionPreResults.flatMap { it.pre.files.map { file -> file.cfirFile } }
        val syntaxPublication = collectParsingDiagnosticPublication(parserFiles, configuration.parsingDiagnosticSettings)
        val syntaxListener = configuration.messageCollector.toCangJieParsingErrorListener()
        installCjmpPackageFeatures(sessionPreResults)

        // 先保留 WARNING：后续宏 token 重解析仍可能产生 LEX ERROR，必须在整个单元收口后统一发布。
        if (syntaxPublication.diagnostics.any { it.isError }) {
            val failedPublication = configuration.diagnosticsCollector.reportParsingDiagnostics(parserFiles, configuration.parsingDiagnosticSettings)
            failedPublication.diagnostics.forEach(syntaxListener::onError)
            return null
        }
        if (configuration.messageCollector.hasErrors()) return null
        val classifications = sessionPreResults.map { (session, pre) ->
            session.ensureAnnotationMetadataRegistry()
            configuration.createMacroDemandClassification(pre).also {
                session.register(MacroDemandClassification::class, it)
            }
        }
        val artifactPreparation = prepareMacroArtifactDefinitionsForExpansion(configuration, classifications)
        val constructionService = FrontendMacroConstructionService(configuration, sdkSelection = artifactPreparation.sdkSelection)

        val constructionOutcomes = sessionPreResults.mapIndexed { index, (session, pre) ->
            val classification = classifications[index]
            classification.freezeFinal(
                macroArtifactDefinitions = artifactPreparation.definitions,
                failurePolicy = when (configuration.macroConstructionMode) {
                    MacroConstructionService.Mode.STRICT -> MacroFailurePolicy.STRICT
                    MacroConstructionService.Mode.DEGRADED -> MacroFailurePolicy.DEGRADED
                },
            )
            buildAndResolveCfirAfterConstruction(
                session = session,
                pre = pre,
                classification = classification,
                constructionService = constructionService,
                constructionMode = configuration.macroConstructionMode,
                macroArtifactDefinitions = artifactPreparation.definitions,
                preConstructionDiagnostics = artifactPreparation.diagnostics,
            )
        }

        val finalSyntaxPublication = configuration.diagnosticsCollector.reportParsingDiagnostics(parserFiles, configuration.parsingDiagnosticSettings)
        finalSyntaxPublication.diagnostics.forEach(syntaxListener::onError)
        constructionOutcomes.forEach { (result, output) ->
            if (output == null) reportConstructionFailure(configuration, result)
        }
        if (configuration.messageCollector.hasErrors()) return null
        val outputs = constructionOutcomes.mapNotNull { it.second }
        for (output in outputs) {
            prepareCjmpImportedSourceContent(output)
            output.session.runCheckers(output.scopeSession, output.fir, configuration.diagnosticsCollector, MppCheckerKind.Common,
                includeParsingDiagnostics = false)
        }

        outputs.runPlatformCheckers(configuration.diagnosticsCollector)
        if (configuration.messageCollector.hasErrors()) return null
        val diagnostics = configuration.diagnosticsCollector.diagnostics.drop(initialDiagnosticCount)
        if (reportBinarySourceDiagnostics(configuration, diagnostics)) return null
        if (configuration.messageCollector.hasErrors()) return null
        if (!writeLiveCjoOutputs(configuration, outputs)) return null

        return DefaultCfirFrontendPipelineArtifact(
            frontendOutput = AllModulesFrontendOutput(outputs),
            configuration = configuration,
            environment = environment,
            sourceFiles = sources.allSources,
        )
    }

    /** 在 raw CFIR 归一化边界按包收集 features，并发布给实际持有 CJO provider 的库会话。 */
    private fun installCjmpPackageFeatures(results: List<SessionPreMacroResult>) {
        for ((librarySession, moduleResults) in results.groupBy { it.librarySession }) {
            val mutableFeaturesByPackage = mutableMapOf<String, MutableSet<String>>()
            for (result in moduleResults) {
                for (file in result.pre.files) {
                    val cfirFile = file.cfirFile
                    val packageName = cfirFile.packageDirective.packageFqName.asString()
                    mutableFeaturesByPackage.getOrPut(packageName) { linkedSetOf() }
                        .addAll(cfirFile.featuresDirective?.featureIds.orEmpty())
                }
            }
            val featuresByPackage = mutableFeaturesByPackage.mapValues { (_, features) -> features.toSet() }
            val settingsSessions: List<CfirSession> = (moduleResults.map { it.session } + listOf(librarySession)).distinct()
            for (session in settingsSessions) {
                session.register(
                    CfirCjmpSettingsComponent::class,
                    session.cjmpSettings.withPackageFeatures(featuresByPackage),
                )
            }
        }
    }

    /**
     * Kotlin FIR writes library metadata after frontend analysis.  CFIR keeps
     * the same phase boundary: this producer consumes resolved CFIR and writes
     * one package artifact per package when an explicit output directory is set.
     */
    private fun writeLiveCjoOutputs(
        configuration: CompilerConfiguration,
        outputs: List<SingleModuleFrontendOutput>,
    ): Boolean {
        val outputDirectory = configuration.cjoOutputDirectory?.let(Path::of)
            ?: configuration.cjoOutputFile?.let { product ->
                val productPath = Path.of(product)
                if (Files.isDirectory(productPath)) productPath else productPath.parent ?: Path.of(".")
            }
        if (outputDirectory == null) return true
        return try {
            Files.createDirectories(outputDirectory)
            val packages = outputs
                .flatMap { output -> output.fir.map { output.session to it } }
                .groupBy { (_, file) -> file.packageDirective.packageFqName.asString() }
            packages.forEach { (packageName, outputFiles) ->
                val sessions = outputFiles.map { it.first }.distinct()
                val files = outputFiles.map { it.second }
                val specificSessions = sessions.filter { it.cjmpSettings.mode == CfirCjmpMode.SPECIFIC }
                val families = specificSessions.map { it.cjmpFamilyAggregate.ensureFamilyComplete(FqName(packageName)) }
                val family = when (families.size) {
                    0 -> null
                    1 -> families.single()
                    else -> CfirCjmpFamilySnapshot.combine(
                        FqName(packageName),
                        families,
                        families.flatMap { it.packageDeclarationsForExport() }.distinctBy { it.symbol },
                    )
                }
                val loadedPackages = CjoExportSourceSnapshots.uniqueLoadedPackages(specificSessions.zip(families).flatMap { (session, snapshot) ->
                    CjoExportSourceSnapshots.loadedPackages(session.dependenciesSymbolProvider, snapshot)
                })
                val metadata = CfirCjoPackageMetadataProducer.produce(
                    files, family, loadedPackages,
                    CjoExportOptions(
                        exportForIncremental = false,
                        exportContent = configuration.nativeMetadataExportConfiguration.importSourceCode,
                        serializingCommon = configuration.cjmpChirOutput,
                        isMacroPackage = files.map { it.packageDirective.isMacroPackage }.distinct().single(),
                        analysisSettings = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings(
                            noSubPackages = configuration.nativeMetadataExportConfiguration.noSubPackages,
                            exportForTest = configuration.nativeMetadataExportConfiguration.exportForTest,
                            canInline = configuration.nativeMetadataExportConfiguration.canInlineSource(configuration.enableCompileTest),
                            compileCjd = configuration.compileCjd,
                        ),
                    ),
                    canonicalSources = listOf(CfirSourceGraphProducer.produce(files, family)),
                )
                val target = outputDirectory.resolve(CjoConstants.packageNameToPath(packageName))
                    Files.createDirectories(target.parent)
                    CjoPackageWriter.write(target, metadata)
                }
            true
        } catch (failure: Throwable) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Cannot produce live CJO metadata: ${failure.message ?: failure::class.simpleName}",
            )
            false
        }
    }

    /**
     * 单个 session 对应的 pre-macro raw build 结果。
     */
    private data class SessionPreMacroResult(
        /**
         * 参与后续 resolve/check 的 CFIR session。
         */
        val session: CfirSession,
        /**
         * 该 session 下源文件构建出的 pre-macro raw 结果。
         */
        val pre: PreMacroRawBuildResult,
        /** 该源码模块依赖的库 session，CJO provider 在此 session 上运行加载门。 */
        val librarySession: CfirSession,
    )

    /**
     * VFS 项目环境和延迟源码收集器。
     */
    private data class EnvironmentAndSources(
        /**
         * 当前阶段使用的项目环境。
         */
        val environment: VfsBasedProjectEnvironment,
        /**
         * 延迟执行的源码收集函数。
         */
        val sources: () -> CollectedCjSources,
    )

    /**
     * 根据 light tree/PSI 配置创建项目环境和源码收集入口。
     */
    private fun createEnvironmentAndSources(
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
    ): EnvironmentAndSources? {
        return when (configuration.useLightTree) {
            true -> {
                val coreEnvironment = CangJieCoreEnvironment.create(rootDisposable, CangJieCoreEnvironmentMode.Production)
                val projectEnvironment = coreEnvironment.toVfsBasedProjectEnvironment()
                val sources = { collectCjSources(configuration, projectEnvironment) }
                EnvironmentAndSources(projectEnvironment, sources)
            }

            false -> {
                val coreEnvironment = CangJieCoreEnvironment.create(rootDisposable, CangJieCoreEnvironmentMode.Production)
                val projectEnvironment = coreEnvironment.toVfsBasedProjectEnvironment()
                val sources = { collectPsiSources(configuration, projectEnvironment) }
                EnvironmentAndSources(projectEnvironment, sources)
            }
        }.takeUnless { CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration) }
    }

    /**
     * 在 PSI 模式下收集仓颉源文件。
     */
    private fun collectPsiSources(
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
    ): CollectedCjSources {
        val platformSources = linkedSetOf<CjSourceFile>()
        val commonSources = linkedSetOf<CjSourceFile>()
        val sourcesByModuleName = linkedMapOf<String, MutableSet<CjSourceFile>>()

        configuration.cangjieSourceRoots.forAllFiles(configuration, environment.project) { virtualFile, isCommon, moduleName ->
            // Keep PSI collection exactly aligned with the LightTree source
            // collector: source recognition and compileCjd acceptance are one
            // owner.  Without this gate PSI can feed `.cj` and `.cj.d` into the
            // same declaration/session, unlike the official compileCjd mode.
            val sourceKind = virtualFile.cangjieSourceKind()
            if (sourceKind == null || !configuration.acceptsCangjieSource(sourceKind)) return@forAllFiles
            val psiFile = com.intellij.psi.PsiManager.getInstance(environment.project).findFile(virtualFile) as? CjFile ?: return@forAllFiles
            // features 的包级收集会首次访问 AST，解析选项必须在源文件收集时已经写入。
            psiFile.cjmpParsingContext = createCfirCjmpSettingsComponent(configuration)
                .toParsingContext(configuration.languageVersionSettings)
            val sourceFile = CjPsiSourceFile(psiFile)
            if (moduleName == null) {
                if (isCommon) commonSources.add(sourceFile) else platformSources.add(sourceFile)
            } else {
                commonSources.add(sourceFile)
                sourcesByModuleName.getOrPut(moduleName) { linkedSetOf() }.add(sourceFile)
            }
        }

        return CollectedCjSources(
            groupedSources = GroupedCjSources(
                platformSources = platformSources,
                commonSources = commonSources,
                sourcesByModuleName = sourcesByModuleName,
            ),
            classpathRoots = configuration.classpathRoots.map { File(it.path) },
        )
    }

    /**
     * 将 core environment 转换为前端 VFS 项目环境。
     */
    private fun CangJieCoreEnvironment.toVfsBasedProjectEnvironment(): VfsBasedProjectEnvironment {
        return VfsBasedProjectEnvironment(
            project = project,
            knownFileSystems = listOf(StandardFileSystems.local(), StandardFileSystems.jar()),
        )
    }

    /**
     * 根据模块分组构建 CFIR sessions。
     */
    private fun buildSessions(
        configuration: CompilerConfiguration,
        rootModuleName: Name,
        groupedSources: GroupedCjSources,
        classpathRoots: List<File>,
        factory: CfirDefaultSessionFactory,
        sessionFactoryContext: CfirDefaultSessionFactory.Context,
        commonPartInputs: CjmpCommonPartInputs,
        extensionRegistrars: List<CfirExtensionRegistrar>,
    ): List<SessionWithSources<CjSourceFile>> {
        val classpathPaths = classpathRoots.map { it.absolutePath }
        // CJMP specific 编译：common part cjo 作为 depends-on 依赖模块（D1 双模块模型，Kotlin
        // `-Xfragment-refines` 对位），精确到文件路径，使其声明带 refinement 依赖模块数据
        val commonPartCjoPaths = commonPartInputs.commonPartCjoPaths
            .map { File(it).absoluteFile.toPath().normalize().toString() }
        val moduleGroups = buildModuleGroups(groupedSources, rootModuleName)

        return moduleGroups.flatMap { (moduleName, moduleSources) ->
            val dependencyList = DependencyListForCliModule.build(moduleName) {
                if (classpathPaths.isNotEmpty()) {
                    dependencies(classpathPaths)
                }
                if (commonPartCjoPaths.isNotEmpty()) {
                    dependsOnDependencies(commonPartCjoPaths)
                }
            }

            CfirSessionConstructionUtils.prepareSessions(
                files = moduleSources.toList(),
                configuration = configuration,
                rootModuleName = moduleName,
                dependencyList = dependencyList,
                createSharedLibrarySession = {
                    factory.createSharedLibrarySession(
                        mainModuleName = moduleName,
                        extensionRegistrars = extensionRegistrars,
                        languageVersionSettings = configuration.languageVersionSettings,
                        context = sessionFactoryContext,
                    )
                },
                createLibrarySession = { sharedLibrarySession ->
                    factory.createLibrarySession(
                        sharedLibrarySession = sharedLibrarySession,
                        moduleDataProvider = dependencyList.moduleDataProvider,
                        extensionRegistrars = extensionRegistrars,
                        languageVersionSettings = configuration.languageVersionSettings,
                        context = sessionFactoryContext,
                    )
                },
                createSourceSession = CfirSessionProducer { _, moduleData, _, sessionConfigurator ->
                    factory.createSourceSession(
                        moduleData = moduleData,
                        extensionRegistrars = extensionRegistrars,
                        configuration = configuration,
                        context = sessionFactoryContext,
                        init = sessionConfigurator,
                    )
                },
            )
        }
    }

    /**
     * 将源码按 root module 和 HMPP module 分组。
     */
    private fun buildModuleGroups(
        groupedSources: GroupedCjSources,
        rootModuleName: Name,
    ): Map<Name, Set<CjSourceFile>> {
        if (groupedSources.sourcesByModuleName.isEmpty()) {
            return mapOf(rootModuleName to groupedSources.allFiles.toSet())
        }

        val groupedByName = groupedSources.sourcesByModuleName.mapKeys { (name, _) -> Name.identifier(name) }
        val assigned = groupedSources.sourcesByModuleName.values.flatten().toSet()
        val unassignedPlatformSources = groupedSources.platformSources.filterNot { it in assigned }.toSet()

        val rootGroup = unassignedPlatformSources + groupedSources.commonSources
        val result = linkedMapOf<Name, Set<CjSourceFile>>(rootModuleName to rootGroup)
        groupedByName.forEach { (name, sources) ->
            result[name] = sources + groupedSources.commonSources
        }
        return result
    }

    /**
     * 按 light tree/PSI 模式构建 pre-macro raw CFIR。
     */
    private fun buildPreMacroFromSources(
        session: CfirSession,
        sources: List<CjSourceFile>,
        environment: VfsBasedProjectEnvironment,
        useLightTree: Boolean,
        parserSettings: org.cangnova.cangjie.parsing.CjParserSettings,
    ): PreMacroRawBuildResult {
        return if (useLightTree) {
            session.buildPreMacroRawCfirViaLightTree(sources, parserSettings = parserSettings)
        } else {
            val files = sources.toCjFiles(environment)
            session.buildPreMacroRawCfirFromCjFiles(files, parserSettings)
        }
    }

    /**
     * 将宏构造失败结果报告到 message collector。
     */
    private fun reportConstructionFailure(
        configuration: CompilerConfiguration,
        result: MacroConstructionResult,
    ) {
        val label = when (result) {
            is MacroConstructionResult.Failed -> "Macro construction failed"
            is MacroConstructionResult.ExecutorUnavailable -> "Macro executor unavailable"
            is MacroConstructionResult.Blocked -> "Macro construction blocked"
            is MacroConstructionResult.Success,
            is MacroConstructionResult.Degraded -> return
        }
        val diagnostics = result.registry.diagnostics
            .filter { it.severity == MacroConstructionDiagnostic.Severity.ERROR }
        // Parser 的成功和失败报告已经与全部源码在单元 owner 完成统一发布。
        if (diagnostics.isEmpty() && result.registry.parsingFailures.isEmpty()) {
            configuration.messageCollector.report(CompilerMessageSeverity.ERROR, "$label: no further details")
            return
        }
        for (diagnostic in diagnostics) {
            val surface = diagnostic.originSurfaceId?.let(result.registry.originSurfaceById::get)
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "$label: ${diagnostic.message}",
                (surface?.sourceRange?.source ?: diagnostic.originSource).toCompilerMessageLocation(),
            )
        }
    }

    /**
     * 将源元素位置转换为编译器消息位置。
     */
    private fun CjSourceElement?.toCompilerMessageLocation(): CompilerMessageSourceLocation? {
        val psi = this?.psi ?: return null
        val containingFile = psi.containingFile ?: return null
        val text = containingFile.text ?: return null
        val start = startOffset.coerceIn(0, text.length)
        val end = endOffset.coerceIn(start, text.length)
        val lineStartOffset = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEndOffset = text.indexOf('\n', start).let { if (it < 0) text.length else it }
        val line = text.take(start).count { it == '\n' } + 1
        val column = start - lineStartOffset + 1
        val lineEnd = text.take(end).count { it == '\n' } + 1
        val lineEndStartOffset = text.lastIndexOf('\n', (end - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val columnEnd = end - lineEndStartOffset + 1
        return CompilerMessageLocationWithRange.create(
            path = containingFile.virtualFile?.path ?: containingFile.name,
            lineStart = line,
            columnStart = column,
            lineEnd = lineEnd,
            columnEnd = columnEnd,
            lineContent = text.substring(lineStartOffset, lineEndOffset),
        )
    }

    /**
     * 将源文件抽象转换为 PSI [CjFile] 列表。
     */
    private fun List<CjSourceFile>.toCjFiles(environment: VfsBasedProjectEnvironment): List<CjFile> {
        return mapNotNull { sourceFile ->
            when (sourceFile) {
                is CjPsiSourceFile -> sourceFile.psiFile as? CjFile
                else -> sourceFile.path
                    ?.let { path -> environment.findFileByPath(path) }
                    ?.let { virtualFile -> com.intellij.psi.PsiManager.getInstance(environment.project).findFile(virtualFile) as? CjFile }
            }
        }
    }
}

/**
 * 将二进制 source element 上的结构化 CFIR 诊断投影到 CLI 消息流。
 *
 * 诊断身份、参数、严重级别和位置均来自 CFIR diagnostics；此处只负责前端 CLI 展示，不执行 CJMP 判定。
 */
private fun reportBinarySourceDiagnostics(
    configuration: CompilerConfiguration,
    diagnostics: List<CjDiagnostic>,
): Boolean {
    var hasErrors = false
    for (diagnostic in diagnostics) {
        val binarySource = (diagnostic as? CjDiagnosticWithSource)?.element as? CjBinarySourceElement ?: continue
        val sourceLocation = binarySource.sourceLocation
        val compilerLocation = sourceLocation?.let {
            CompilerMessageLocation.create(it.filePath, it.line, it.column, null)
        } ?: CompilerMessageLocation.create(binarySource.binaryFilePath)
        configuration.messageCollector.report(
            severity = diagnostic.severity.toCompilerMessageSeverity(),
            message = diagnostic.renderMessage(),
            location = compilerLocation,
        )
        hasErrors = hasErrors || diagnostic.severity.isError
    }
    return hasErrors
}
