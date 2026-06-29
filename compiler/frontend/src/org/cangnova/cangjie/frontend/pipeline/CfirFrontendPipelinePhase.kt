package org.cangnova.cangjie.frontend.pipeline

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.entrypoint.session.createDefaultCfirSessionFactoryContext
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.lightTree.LightTreeRawCfirDeclarationBuilder
import org.cangnova.cangjie.cfir.pipeline.AllModulesFrontendOutput
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.pipeline.SessionWithSources
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirFromCjFiles
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirViaLightTree
import org.cangnova.cangjie.cfir.pipeline.resolveAndCheckCfir
import org.cangnova.cangjie.cfir.pipeline.resolveAndCheckCfirAfterConstruction
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFailurePolicy
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.TokenBackedMacroFragmentParser
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cangjieSourceRoots
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.environment.VfsBasedProjectEnvironment
import org.cangnova.cangjie.frontend.environment.findFileByPath
import org.cangnova.cangjie.frontend.environment.forAllFiles
import org.cangnova.cangjie.frontend.sources.CollectedCjSources
import org.cangnova.cangjie.frontend.sources.GroupedCjSources
import org.cangnova.cangjie.frontend.sources.allFiles
import org.cangnova.cangjie.frontend.sources.collectCjSources
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CompilerMessageLocationWithRange
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.readSourceFileWithMapping
import java.io.File

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
        val sessionFactoryContext = createDefaultCfirSessionFactoryContext(configuration)
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
            extensionRegistrars = extensionRegistrars,
        )

        val sessionPreResults = sessionsWithSources.map { (session, sessionSources) ->
            SessionPreMacroResult(
                session = session,
                pre = buildPreMacroFromSources(
                    session = session,
                    sources = sessionSources,
                    environment = environment,
                    useLightTree = configuration.useLightTree,
                ),
            )
        }

        val constructionService = FrontendMacroConstructionService(configuration)
        val classifications = sessionPreResults.map { (session, pre) ->
            session.ensureAnnotationMetadataRegistry()
            MacroDemandClassification.create(pre).also {
                session.register(MacroDemandClassification::class, it)
            }
        }
        val artifactPreparation = prepareMacroArtifactDefinitionsForExpansion(configuration, classifications)

        val outputs = sessionPreResults.mapIndexedNotNull { index, (session, pre) ->
            val classification = classifications[index]
            classification.freezeFinal(
                macroArtifactDefinitions = artifactPreparation.definitions,
                failurePolicy = when (configuration.macroConstructionMode) {
                    MacroConstructionService.Mode.STRICT -> MacroFailurePolicy.STRICT
                    MacroConstructionService.Mode.DEGRADED -> MacroFailurePolicy.DEGRADED
                },
            )
            val (result, output) = resolveAndCheckCfirAfterConstruction(
                session = session,
                pre = pre,
                classification = classification,
                constructionService = constructionService,
                constructionMode = configuration.macroConstructionMode,
                diagnosticsCollector = configuration.diagnosticsCollector,
                macroArtifactDefinitions = artifactPreparation.definitions,
                preConstructionDiagnostics = artifactPreparation.diagnostics,
            )
            if (output == null) {
                reportConstructionFailure(configuration, result)
            }
            output
        }

        return DefaultCfirFrontendPipelineArtifact(
            frontendOutput = AllModulesFrontendOutput(outputs),
            configuration = configuration,
            environment = environment,
            sourceFiles = sources.allSources,
        )
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
            val psiFile = com.intellij.psi.PsiManager.getInstance(environment.project).findFile(virtualFile) as? CjFile ?: return@forAllFiles
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
        extensionRegistrars: List<CfirExtensionRegistrar>,
    ): List<SessionWithSources<CjSourceFile>> {
        val classpathPaths = classpathRoots.map { it.absolutePath }
        val moduleGroups = buildModuleGroups(groupedSources, rootModuleName)

        return moduleGroups.flatMap { (moduleName, moduleSources) ->
            val dependencyList = DependencyListForCliModule.build(moduleName) {
                if (classpathPaths.isNotEmpty()) {
                    dependencies(classpathPaths)
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
    ): PreMacroRawBuildResult {
        return if (useLightTree) {
            session.buildPreMacroRawCfirViaLightTree(sources)
        } else {
            session.buildPreMacroRawCfirFromCjFiles(sources.toCjFiles(environment))
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
            .filter { it.severity == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Severity.ERROR }
        if (diagnostics.isEmpty()) {
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
