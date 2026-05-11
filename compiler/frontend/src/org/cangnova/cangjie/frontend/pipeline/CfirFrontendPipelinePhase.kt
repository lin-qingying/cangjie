package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.entrypoint.configuration.initializeCfirFrontendConfiguration
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.pipeline.AllModulesFrontendOutput
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.pipeline.SessionWithSources
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirFromCjFiles
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirViaLightTree
import org.cangnova.cangjie.cfir.pipeline.resolveAndCheckCfir
import org.cangnova.cangjie.cfir.pipeline.resolveAndCheckCfirAfterConstruction
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
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
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.readSourceFileWithMapping
import java.io.File

object CfirFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, DefaultCfirFrontendPipelineArtifact>(
    name = "CfirFrontendPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector),
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): DefaultCfirFrontendPipelineArtifact? {
        val (configuration, rootDisposable) = input
        configuration.initializeCfirFrontendConfiguration()

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
        val sessionFactoryContext = CfirDefaultSessionFactory.Context()
        val extensionRegistrars = emptyList<CfirExtensionRegistrar>()

        // Baseline 第 1 节"主流程"：
        //   pre → MacroConstructionService.expand → recordExpandedRawFilesOnce → resolve & check
        //
        // 当前 batch 仍以 STRICT 模式驱动 CLI：构造失败立刻终止该 module 的 resolve。
        val constructionService = FrontendMacroConstructionService(configuration)

        val sessionsWithSources = buildSessions(
            configuration = configuration,
            rootModuleName = rootModuleName,
            groupedSources = sources.groupedSources,
            classpathRoots = sources.classpathRoots,
            factory = factory,
            sessionFactoryContext = sessionFactoryContext,
            extensionRegistrars = extensionRegistrars,
        )

        val outputs = sessionsWithSources.mapNotNull { (session, sessionSources) ->
            val pre = buildPreMacroFromSources(
                session = session,
                sources = sessionSources,
                environment = environment,
                useLightTree = configuration.useLightTree,
            )
            val (result, output) = resolveAndCheckCfirAfterConstruction(
                session = session,
                pre = pre,
                constructionService = constructionService,
                constructionMode = MacroConstructionService.Mode.STRICT,
                diagnosticsCollector = configuration.diagnosticsCollector,
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

    private data class EnvironmentAndSources(
        val environment: VfsBasedProjectEnvironment,
        val sources: () -> CollectedCjSources,
    )

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

    private fun CangJieCoreEnvironment.toVfsBasedProjectEnvironment(): VfsBasedProjectEnvironment {
        return VfsBasedProjectEnvironment(
            project = project,
            knownFileSystems = listOf(StandardFileSystems.local(), StandardFileSystems.jar()),
        )
    }

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
        val details = result.registry.diagnostics
            .filter { it.severity == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Severity.ERROR }
            .joinToString("; ") { it.message }
            .ifEmpty { "no further details" }
        configuration.messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "$label: $details",
        )
    }

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
