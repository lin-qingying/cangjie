package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.checkers.registerExperimentalCheckers
import org.cangnova.cangjie.cfir.entrypoint.checkers.registerExtraCommonCheckers
import org.cangnova.cangjie.cfir.entrypoint.configuration.apiLevel
import org.cangnova.cangjie.cfir.entrypoint.configuration.apiLevelSyscapConfigPath
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.entrypoint.session.CfirSessionConfigurator
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirApiLevelProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.frontend.pipeline.initializeCfirFrontendMacroCompilationConfiguration
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
import java.util.regex.Pattern

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
        configuration.initializeCfirFrontendMacroCompilationConfiguration()
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
        configuration.initializeCfirFrontendMacroCompilationConfiguration()
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
            configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module),
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
        configuration.initializeCfirFrontendMacroCompilationConfiguration()
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
        val apiLevelProvider = createApiLevelProvider(configuration)
        return CfirDefaultSessionFactory.Context(
            cjoManager = cjoManager,
            registerSourceSessionComponents = {
                if (apiLevelProvider != null) {
                    register(CfirApiLevelProvider::class, apiLevelProvider)
                }
            },
        )
    }

    /**
     * 为 LLT 迁移后的测试数据注入稳定的 API level/syscap 配置。
     *
     * 这里显式从测试指令构建 [CfirApiLevelProvider]，避免依赖当前 CFIR
     * 产物去“倒推”预期诊断。
     */
    private fun createApiLevelProvider(configuration: CompilerConfiguration): CfirApiLevelProvider? {
        val projectApiLevel = configuration.apiLevel
        val syscapConfigPath = configuration.apiLevelSyscapConfigPath

        if (projectApiLevel == null && syscapConfigPath.isNullOrBlank()) {
            return null
        }

        val syscapInfo = syscapConfigPath
            ?.takeIf { it.isNotBlank() }
            ?.let(::parseSyscapConfiguration)
            ?: ParsedSyscapConfiguration.EMPTY

        return object : CfirApiLevelProvider {
            override val projectApiLevel: Int =
                projectApiLevel ?: syscapInfo.apiLevel ?: CfirApiLevelProvider.DISABLED
            override val syscapEnabled: Boolean =
                syscapInfo.union.isNotEmpty() || syscapInfo.intersection.isNotEmpty()
            override val syscapUnion: Set<String> = syscapInfo.union
            override val syscapIntersection: Set<String> = syscapInfo.intersection
        }
    }

    private fun parseSyscapConfiguration(rawPath: String): ParsedSyscapConfiguration {
        val configFile = File(rawPath)
        if (!configFile.exists() || !configFile.isFile) {
            return ParsedSyscapConfiguration.EMPTY
        }

        val content = runCatching { configFile.readText() }.getOrDefault("")
        if (content.isBlank()) return ParsedSyscapConfiguration.EMPTY

        val apiLevel = API_LEVEL_REGEX.find(content)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val referencedFiles = SYS_CAP_FILE_REGEX.findAll(content)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .map { relativePath -> configFile.parentFile.resolve(relativePath).normalize() }
            .filter { it.exists() && it.isFile }
            .toList()

        if (referencedFiles.isEmpty()) {
            return ParsedSyscapConfiguration(apiLevel = apiLevel)
        }

        val syscapSets = referencedFiles.mapNotNull(::parseSyscapLeafFile)
        if (syscapSets.isEmpty()) {
            return ParsedSyscapConfiguration(apiLevel = apiLevel)
        }

        val union = linkedSetOf<String>()
        syscapSets.forEach { union += it }

        val intersection = syscapSets
            .drop(1)
            .fold(syscapSets.first().toSet()) { acc, next -> acc intersect next }

        return ParsedSyscapConfiguration(
            apiLevel = apiLevel,
            union = union,
            intersection = intersection,
        )
    }

    private fun parseSyscapLeafFile(file: File): Set<String>? {
        val content = runCatching { file.readText() }.getOrDefault("")
        if (content.isBlank()) return null
        val values = SYS_CAP_VALUE_REGEX.findAll(content)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toCollection(linkedSetOf())
        return values.takeIf { it.isNotEmpty() }
    }

    companion object {
        private val API_LEVEL_REGEX = Pattern.compile("\"apiLevel\"\\s*:\\s*(\\d+)").toRegex()
        private val SYS_CAP_FILE_REGEX = Pattern.compile("\"(\\./[^\"]+\\.json)\"").toRegex()
        private val SYS_CAP_VALUE_REGEX = Pattern.compile("\"([^\"]+)\"").toRegex()

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

private data class ParsedSyscapConfiguration(
    val apiLevel: Int? = null,
    val union: Set<String> = emptySet(),
    val intersection: Set<String> = emptySet(),
) {
    companion object {
        val EMPTY = ParsedSyscapConfiguration()
    }
}

private fun TestModule.sourceDependencyModules(): List<TestModule> {
    return allDependencies
        .filter { it.kind == Source }
        .mapNotNull(DependencyDescription::dependencyModule)
}
