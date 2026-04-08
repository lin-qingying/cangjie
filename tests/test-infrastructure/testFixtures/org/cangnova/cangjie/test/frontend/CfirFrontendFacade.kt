package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.checkers.registerExperimentalCheckers
import org.cangnova.cangjie.cfir.entrypoint.checkers.registerExtraCommonCheckers
import org.cangnova.cangjie.cfir.entrypoint.configuration.initializeCfirFrontendConfiguration
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.entrypoint.session.CfirSessionConfigurator
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.singleValue
import org.cangnova.cangjie.test.model.DependencyDescription
import org.cangnova.cangjie.test.model.DependencyKind.Source
import org.cangnova.cangjie.test.model.FrontendFacade
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.ServiceRegistrationData
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.frontendBasedFacadesMarkerRegistrationData
import org.cangnova.cangjie.test.services.compilerConfigurationProvider
import org.cangnova.cangjie.test.services.defaultsProvider
import org.cangnova.cangjie.test.services.getCjFilesForSourceFiles
import org.cangnova.cangjie.test.services.getCjSourceFilesForSourceFiles
import org.cangnova.cangjie.test.services.service
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.cangnova.cangjie.utils.DFS
import java.io.File

/**
 * CFIR frontend facade for test infrastructure.
 *
 * Mirrors Kotlin FIR facade architecture while using Cangjie single-platform facilities.
 */
open class CfirFrontendFacade(
    testServices: TestServices,
) : FrontendFacade<CfirOutputArtifact>(testServices, FrontendKinds.CFIR) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            frontendBasedFacadesMarkerRegistrationData,
            service(::CfirModuleInfoProvider),
            service(::CfirDiagnosticCollectorService),
        )

    override val directiveContainers
        get() = listOf(CfirDiagnosticsDirectives)

    override fun shouldTransform(module: TestModule): Boolean {
        return shouldRunCfirFrontendFacade(module, testServices)
    }

    override fun analyze(module: TestModule): CfirOutputArtifact {
        val sortedModules = sortDependsOnTopologically(module)
        val (moduleDataMap, moduleDataProvider) = initializeModuleData(sortedModules)

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        configuration.initializeCfirFrontendConfiguration()
        val extensionRegistrars = emptyList<CfirExtensionRegistrar>()
        val sessionFactoryContext = createSessionFactoryContext(configuration)
        val librarySession = createLibrarySession(
            module,
            Name.special("<${module.name}>"),
            moduleDataProvider,
            configuration,
            extensionRegistrars,
            sessionFactoryContext,
        )

        val firOutputPartForDependsOnModules = sortedModules.map {
            analyze(
                module = it,
                moduleData = moduleDataMap.getValue(it),
                librarySession = librarySession,
                extensionRegistrars = extensionRegistrars,
                sessionFactoryContext = sessionFactoryContext,
            )
        }

        return CfirOutputArtifactImpl(firOutputPartForDependsOnModules)
    }

    protected fun sortDependsOnTopologically(module: TestModule): List<TestModule> {
        val reachableModules = linkedSetOf<TestModule>()

        fun collect(current: TestModule) {
            if (!reachableModules.add(current)) return
            current.sourceDependencyModules().forEach(::collect)
        }

        collect(module)

        return DFS.topologicalOrder(reachableModules) { current ->
            current.sourceDependencyModules().filter { it in reachableModules }
        }.asReversed()
    }

    private fun initializeModuleData(
        modules: List<TestModule>,
    ): Pair<Map<TestModule, CfirModuleData>, ModuleDataProvider> {
        val mainModule = modules.last()
        val moduleName = Name.special("<${mainModule.name}>")
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(mainModule)
        val libraryList = initializeLibraryList(mainModule, moduleName, configuration, testServices)

        val moduleInfoProvider = testServices.cfirModuleInfoProvider
        val moduleDataMap = mutableMapOf<TestModule, CfirModuleData>()

        for (module in modules) {
            val regularModules = libraryList.regularDependencies +
                moduleInfoProvider.getRegularDependentSourceModules(module) +
                moduleInfoProvider.getDependentFriendSourceModules(module)
            val dependsOnModules = libraryList.dependsOnDependencies +
                moduleInfoProvider.getDependentDependsOnSourceModules(module)

            val moduleData = CfirSourceModuleData(
                name = Name.special("<${module.name}>"),
                dependencies = regularModules,
                refinementDependencies = dependsOnModules,
                platform = CfirPlatform.DEFAULT,
                isCommon = false,
            )
            moduleInfoProvider.registerModuleData(module, moduleData)
            moduleDataMap[module] = moduleData
        }

        return moduleDataMap to libraryList.moduleDataProvider
    }

    private fun createLibrarySession(
        module: TestModule,
        moduleName: Name,
        moduleDataProvider: ModuleDataProvider,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        sessionFactoryContext: CfirDefaultSessionFactory.Context,
    ): CfirSession {
        val languageVersionSettings = module.languageVersionSettings ?: configuration.languageVersionSettings
        configuration.initializeCfirFrontendConfiguration()
        val factory = CfirDefaultSessionFactory()
        val sharedLibrarySession = factory.createSharedLibrarySession(
            mainModuleName = moduleName,
            extensionRegistrars = extensionRegistrars,
            languageVersionSettings = languageVersionSettings,
            context = sessionFactoryContext,
        )
        return factory.createLibrarySession(
            sharedLibrarySession = sharedLibrarySession,
            moduleDataProvider = moduleDataProvider,
            extensionRegistrars = extensionRegistrars,
            languageVersionSettings = languageVersionSettings,
            context = sessionFactoryContext,
        )
    }

    private fun analyze(
        module: TestModule,
        moduleData: CfirModuleData,
        librarySession: CfirSession,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        sessionFactoryContext: CfirDefaultSessionFactory.Context,
    ): CfirOutputPartForDependsOnModule {
        val project = testServices.compilerConfigurationProvider.getProject(module)
        val parser = module.directives[CfirDiagnosticsDirectives.CFIR_PARSER].lastOrNull() ?: CfirParser.LightTree

        val (cjFiles, lightTreeFiles) = when (parser) {
            CfirParser.LightTree -> {
                emptyMap<TestFile, CjFile>() to testServices.sourceFileProvider.getCjSourceFilesForSourceFiles(module.files)
            }

            CfirParser.Psi -> testServices.sourceFileProvider.getCjFilesForSourceFiles(module.files, project) to emptyMap()
        }

        val sessionConfigurator: CfirSessionConfigurator.() -> Unit = {
            testServices.cfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar
                ?.registerAdditionalComponent(this)

            if (CfirDiagnosticsDirectives.WITH_EXTRA_CHECKERS in module.directives) {
                registerExtraCommonCheckers()
            }
            if (CfirDiagnosticsDirectives.WITH_EXPERIMENTAL_CHECKERS in module.directives) {
                registerExperimentalCheckers()
            }
        }

        val moduleBasedSession = createModuleBasedSession(
            module = module,
            moduleData = moduleData,
            extensionRegistrars = extensionRegistrars,
            sessionConfigurator = sessionConfigurator,
            sessionFactoryContext = sessionFactoryContext,
        )

        val firAnalyzerFacade = CfirAnalyzerFacade(
            session = moduleBasedSession,
            cjFiles = cjFiles.values,
            lightTreeFiles = lightTreeFiles.values,
            parser = parser,
            diagnosticReporterForLightTree = testServices.cfirDiagnosticCollectorService.reporterForLTSyntaxErrors,
        )
        val firFiles = firAnalyzerFacade.runResolution()

        val usedFilesMap = when (parser) {
            CfirParser.LightTree -> lightTreeFiles
            CfirParser.Psi -> cjFiles
        }

        val filesMap = usedFilesMap.keys
            .zip(firFiles)
            .onEach { assert(it.first.name == it.second.name) }
            .toMap()

        return CfirOutputPartForDependsOnModule(
            module = module,
            session = moduleBasedSession,
            scopeSession = firAnalyzerFacade.scopeSession,
            firAnalyzerFacade = firAnalyzerFacade,
            firFilesByTestFile = filesMap,
        )
    }

    private fun createModuleBasedSession(
        module: TestModule,
        moduleData: CfirModuleData,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        sessionConfigurator: CfirSessionConfigurator.() -> Unit,
        sessionFactoryContext: CfirDefaultSessionFactory.Context,
    ): CfirSession {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        module.languageVersionSettings?.let { configuration.languageVersionSettings = it }
        configuration.initializeCfirFrontendConfiguration()
        val factory = CfirDefaultSessionFactory()
        return factory.createSourceSession(
            moduleData = moduleData,
            extensionRegistrars = extensionRegistrars,
            configuration = configuration,
            context = sessionFactoryContext,
            init = sessionConfigurator,
        )
    }

    private fun createSessionFactoryContext(configuration: CompilerConfiguration): CfirDefaultSessionFactory.Context {
        val classpath = configuration.classpathRoots.map { it.path }.filter { it.isNotBlank() }
        val cjoManager = CjoManager(
            CjoSearchPath { key ->
                when (key) {
                    "CANGJIE_LIBRARY" -> classpath.takeIf { it.isNotEmpty() }?.joinToString(File.pathSeparator)
                    else -> System.getenv(key)
                }
            }
        )
        return CfirDefaultSessionFactory.Context(cjoManager = cjoManager)
    }

    companion object {
        fun initializeLibraryList(
            @Suppress("UNUSED_PARAMETER") mainModule: TestModule,
            mainModuleName: Name,
            configuration: CompilerConfiguration,
            @Suppress("UNUSED_PARAMETER") testServices: TestServices,
        ): DependencyListForCliModule {
            return DependencyListForCliModule.build {
                defaultDependenciesSet(mainModuleName) {
                    dependencies(configuration.classpathRoots.map { it.path })
                }
            }
        }

        fun shouldRunCfirFrontendFacade(
            @Suppress("UNUSED_PARAMETER") module: TestModule,
            testServices: TestServices,
        ): Boolean {
            return testServices.defaultsProvider.frontendKind == FrontendKinds.CFIR
        }
    }
}

private fun TestModule.sourceDependencyModules(): List<TestModule> {
    return allDependencies
        .filter { it.kind == Source }
        .mapNotNull(DependencyDescription::dependencyModule)
}
