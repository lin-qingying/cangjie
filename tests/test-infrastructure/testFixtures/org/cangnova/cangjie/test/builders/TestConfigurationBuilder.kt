package org.cangnova.cangjie.test.builders

import com.intellij.openapi.Disposable
import org.cangnova.cangjie.test.CangJieTestInfo
import org.cangnova.cangjie.test.Constructor
import org.cangnova.cangjie.test.GroupingPhaseInputsMerger
import org.cangnova.cangjie.test.GroupingPhaseTestConfiguration
import org.cangnova.cangjie.test.GroupingPhaseTestConfigurationImpl

import org.cangnova.cangjie.test.NonGroupingPhaseTestConfiguration
import org.cangnova.cangjie.test.NonGroupingPhaseTestConfigurationImpl
import org.cangnova.cangjie.test.TestDisposable
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.TestStep
import org.cangnova.cangjie.test.UpdateTestDataHandler
import org.cangnova.cangjie.test.config.DefaultsProviderBuilder
import org.cangnova.cangjie.test.directives.model.ComposedDirectivesContainer
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectivesBuilder
import org.cangnova.cangjie.test.model.AbstractGroupingPhaseTestFacade
import org.cangnova.cangjie.test.model.AbstractTestFacade
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.ServicesAndDirectivesContainer
import org.cangnova.cangjie.test.model.TestArtifactKind
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.AbstractEnvironmentConfigurator
import org.cangnova.cangjie.test.services.AdditionalMetaInfoProcessor
import org.cangnova.cangjie.test.services.AdditionalSourceProvider
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.test.services.CompilationStage
import org.cangnova.cangjie.test.services.CompilerConfigurationProvider
import org.cangnova.cangjie.test.services.DefaultsDsl
import org.cangnova.cangjie.test.services.ModuleStructureExtractorImpl
import org.cangnova.cangjie.test.services.ModuleStructureTransformer
import org.cangnova.cangjie.test.services.PreAnalysisHandler
import org.cangnova.cangjie.test.services.RuntimeClasspathProvider
import org.cangnova.cangjie.test.services.ServiceRegistrationData
import org.cangnova.cangjie.test.services.SourceFilePreprocessor
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.impl.DefaultAssertionsService
import org.cangnova.cangjie.test.services.service
import java.nio.file.Path

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class)
abstract class TestConfigurationBuilderBase<Self : TestConfigurationBuilderBase<Self, C>, C> {
    val defaultsProviderBuilder: DefaultsProviderBuilder = DefaultsProviderBuilder()
    var assertions: AssertionsService = DefaultAssertionsService()

    protected val sourcePreprocessors = mutableListOf<Constructor<SourceFilePreprocessor>>()
    protected val additionalMetaInfoProcessors = mutableListOf<Constructor<AdditionalMetaInfoProcessor>>()
    protected val environmentConfigurators = mutableListOf<Constructor<AbstractEnvironmentConfigurator>>()
    protected val preAnalysisHandlers = mutableListOf<Constructor<PreAnalysisHandler>>()

    protected val additionalSourceProviders = mutableListOf<Constructor<AdditionalSourceProvider>>()
    protected val moduleStructureTransformers = mutableListOf<Constructor<ModuleStructureTransformer>>()

    protected val metaTestConfigurators = mutableListOf<Constructor<org.cangnova.cangjie.test.services.MetaTestConfigurator>>()
    protected val afterAnalysisCheckers = mutableListOf<Constructor<AfterAnalysisChecker>>()

    protected var metaInfoHandlerEnabled: Boolean = false

    protected val directives = mutableListOf<DirectivesContainer>()
    val defaultRegisteredDirectivesBuilder = RegisteredDirectivesBuilder()

    protected val additionalServices = mutableListOf<ServiceRegistrationData>()

    protected var compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)? =
        null
    protected val runtimeClasspathProviders = mutableListOf<Constructor<RuntimeClasspathProvider>>()

    inline fun <reified T : TestService> useAdditionalService(noinline serviceConstructor: (TestServices) -> T) {
        useAdditionalServices(service(serviceConstructor))
    }

    fun useAdditionalServices(vararg serviceRegistrationData: ServiceRegistrationData) {
        additionalServices += serviceRegistrationData
    }

    open fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        defaultsProviderBuilder.apply(init)
    }

    fun useSourcePreprocessor(vararg preprocessors: Constructor<SourceFilePreprocessor>, needToPrepend: Boolean = false) {
        if (needToPrepend) {
            sourcePreprocessors.addAll(0, preprocessors.toList())
        } else {
            sourcePreprocessors.addAll(preprocessors)
        }
    }

    fun useDirectives(vararg directives: DirectivesContainer) {
        this.directives += directives
    }

    fun useConfigurators(vararg environmentConfigurators: Constructor<AbstractEnvironmentConfigurator>) {
        this.environmentConfigurators += environmentConfigurators
    }

    fun usePreAnalysisHandlers(vararg handlers: Constructor<PreAnalysisHandler>) {
        this.preAnalysisHandlers += handlers
    }

    fun useMetaInfoProcessors(vararg processors: Constructor<AdditionalMetaInfoProcessor>) {
        additionalMetaInfoProcessors += processors
    }

    fun useAdditionalSourceProviders(vararg providers: Constructor<AdditionalSourceProvider>) {
        additionalSourceProviders += providers
    }

    @TestInfrastructureInternals
    fun useModuleStructureTransformers(vararg transformers: Constructor<ModuleStructureTransformer>) {
        moduleStructureTransformers += transformers
    }

    @TestInfrastructureInternals
    fun useCustomCompilerConfigurationProvider(provider: (TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider) {
        compilerConfigurationProvider = provider
    }

    fun useCustomRuntimeClasspathProviders(vararg provider: Constructor<RuntimeClasspathProvider>) {
        runtimeClasspathProviders += provider
    }

    fun useMetaTestConfigurators(vararg configurators: Constructor<org.cangnova.cangjie.test.services.MetaTestConfigurator>) {
        metaTestConfigurators += configurators
    }

    fun useAfterAnalysisCheckers(vararg checkers: Constructor<AfterAnalysisChecker>, insertAtFirst: Boolean = false) {
        if (insertAtFirst) {
            afterAnalysisCheckers.addAll(0, checkers.toList())
        } else {
            afterAnalysisCheckers += checkers
        }
    }

    open fun defaultDirectives(init: RegisteredDirectivesBuilder.() -> Unit) {
        defaultRegisteredDirectivesBuilder.apply(init)
    }

    abstract fun build(testDataPath: String): C
}

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class)
class NonGroupingPhaseTestConfigurationBuilder :
    TestConfigurationBuilderBase<NonGroupingPhaseTestConfigurationBuilder, NonGroupingPhaseTestConfiguration>() {
    lateinit var testInfo: CangJieTestInfo
    var startingArtifactFactory: (TestModule) -> ResultingArtifact<*> = { ResultingArtifact.Source() }

    private val steps = mutableListOf<TestStepBuilder<*, *, TestStep.NonGroupingStep<*, *>>>()
    private val namedSteps = mutableMapOf<String, TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<*, *>>()

    private val globalDefaultsConfigurators = mutableListOf<DefaultsProviderBuilder.() -> Unit>()
    private val defaultDirectiveConfigurators = mutableListOf<RegisteredDirectivesBuilder.() -> Unit>()
    private val configurationsByPositiveTestDataCondition = mutableListOf<Pair<Regex, NonGroupingPhaseTestConfigurationBuilder.() -> Unit>>()
    private val configurationsByNegativeTestDataCondition = mutableListOf<Pair<Regex, NonGroupingPhaseTestConfigurationBuilder.() -> Unit>>()

    fun forTestsMatching(pattern: String, configuration: NonGroupingPhaseTestConfigurationBuilder.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        configurationsByPositiveTestDataCondition += regex to configuration
    }

    fun forTestsNotMatching(pattern: String, configuration: NonGroupingPhaseTestConfigurationBuilder.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        configurationsByNegativeTestDataCondition += regex to configuration
    }

    infix fun String.or(other: String): String = "$this|$other"

    private fun String.toMatchingRegexString(): String = when (this) {
        "*" -> ".*"
        else -> "^.*/(${replace("*", ".*")})$"
    }

    override fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        globalDefaultsConfigurators += init
        super.globalDefaults(init)
    }

    override fun defaultDirectives(init: RegisteredDirectivesBuilder.() -> Unit) {
        defaultDirectiveConfigurators += init
        super.defaultDirectives(init)
    }

    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractTestFacade<I, O>>,
    ): TestStepBuilder.FacadeStepBuilder.NonGroupingPhase<I, O> {
        return TestStepBuilder.FacadeStepBuilder.NonGroupingPhase(facade).also { steps += it }
    }

    fun <InputArtifact, InputArtifactKind> handlersStep(
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        return TestStepBuilder.HandlersStepBuilder.NonGroupingPhase(artifactKind, compilationStage).also {
            it.init()
            steps += it
        }
    }

    fun <InputArtifact, InputArtifactKind> namedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        @Suppress("UNCHECKED_CAST")
        val existing = namedSteps[name] as TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<InputArtifact, InputArtifactKind>?
        if (existing != null) {
            existing.init()
            return existing
        }
        val created = handlersStep(artifactKind, compilationStage, init)
        namedSteps[name] = created
        return created
    }

    fun <InputArtifact, InputArtifactKind> configureNamedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        skipMissingStep: Boolean = false,
        init: TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ) where InputArtifact : ResultingArtifact<InputArtifact>,
            InputArtifactKind : TestArtifactKind<InputArtifact> {
        @Suppress("UNCHECKED_CAST")
        val existing = namedSteps[name] as TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<InputArtifact, InputArtifactKind>?
        if (existing == null) {
            if (skipMissingStep) return
            error("Step \"$name\" not found")
        }
        require(existing.artifactKind == artifactKind) { "Step kind: ${existing.artifactKind}, passed kind is $artifactKind" }
        existing.init()
    }

    fun enableMetaInfoHandler() {
        metaInfoHandlerEnabled = true
    }

    override fun build(testDataPath: String): NonGroupingPhaseTestConfiguration {
        val absoluteTestDataPath = Path.of(testDataPath).normalize().toUri().toString()

        for ((regex, configuration) in configurationsByPositiveTestDataCondition) {
            if (regex.matches(absoluteTestDataPath)) {
                this.configuration()
            }
        }
        for ((regex, configuration) in configurationsByNegativeTestDataCondition) {
            if (!regex.matches(absoluteTestDataPath)) {
                this.configuration()
            }
        }

        useAfterAnalysisCheckers(::UpdateTestDataHandler)

        return NonGroupingPhaseTestConfigurationImpl(
            testInfo = testInfo,
            defaultsProvider = defaultsProviderBuilder.build(),
            assertions = assertions,
            steps = steps,
            sourcePreprocessors = sourcePreprocessors,
            additionalMetaInfoProcessors = additionalMetaInfoProcessors,
            environmentConfigurators = environmentConfigurators,
            additionalSourceProviders = additionalSourceProviders,
            preAnalysisHandlers = preAnalysisHandlers,
            moduleStructureTransformers = moduleStructureTransformers,
            metaTestConfigurators = metaTestConfigurators,
            afterAnalysisCheckers = afterAnalysisCheckers,
            compilerConfigurationProvider = compilerConfigurationProvider,
            runtimeClasspathProviders = runtimeClasspathProviders,
            metaInfoHandlerEnabled = metaInfoHandlerEnabled,
            directives = directives,
            defaultRegisteredDirectives = defaultRegisteredDirectivesBuilder.build(),
            startingArtifactFactory = startingArtifactFactory,
            additionalServices = additionalServices,
            originalBuilder = ReadOnlyBuilder(this, testDataPath)
        )
    }

    class ReadOnlyBuilder(private val builder: NonGroupingPhaseTestConfigurationBuilder, val testDataPath: String) {
        val defaultsProviderBuilder: DefaultsProviderBuilder
            get() = builder.defaultsProviderBuilder
        val assertions: AssertionsService
            get() = builder.assertions
        val sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>
            get() = builder.sourcePreprocessors
        val additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>
            get() = builder.additionalMetaInfoProcessors
        val environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>
            get() = builder.environmentConfigurators
        val preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>
            get() = builder.preAnalysisHandlers
        val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>
            get() = builder.additionalSourceProviders
        val moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>
            get() = builder.moduleStructureTransformers
        val metaTestConfigurators: List<Constructor<org.cangnova.cangjie.test.services.MetaTestConfigurator>>
            get() = builder.metaTestConfigurators
        val afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
            get() = builder.afterAnalysisCheckers
        val metaInfoHandlerEnabled: Boolean
            get() = builder.metaInfoHandlerEnabled
        val directives: List<DirectivesContainer>
            get() = builder.directives
        val defaultDirectiveConfigurators: List<RegisteredDirectivesBuilder.() -> Unit>
            get() = builder.defaultDirectiveConfigurators
        val globalDefaultsConfigurators: List<DefaultsProviderBuilder.() -> Unit>
            get() = builder.globalDefaultsConfigurators
        val configurationsByPositiveTestDataCondition: List<Pair<Regex, NonGroupingPhaseTestConfigurationBuilder.() -> Unit>>
            get() = builder.configurationsByPositiveTestDataCondition
        val configurationsByNegativeTestDataCondition: List<Pair<Regex, NonGroupingPhaseTestConfigurationBuilder.() -> Unit>>
            get() = builder.configurationsByNegativeTestDataCondition
        val additionalServices: List<ServiceRegistrationData>
            get() = builder.additionalServices
        val compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?
            get() = builder.compilerConfigurationProvider
        val runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>
            get() = builder.runtimeClasspathProviders
        val testInfo: CangJieTestInfo
            get() = builder.testInfo
        val startingArtifactFactory: (TestModule) -> ResultingArtifact<*>
            get() = builder.startingArtifactFactory
    }
}

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class)
class GroupingPhaseTestConfigurationBuilder :
    TestConfigurationBuilderBase<GroupingPhaseTestConfigurationBuilder, GroupingPhaseTestConfiguration>() {
    lateinit var testInfo: CangJieTestInfo
    private val mergerWorkers = mutableListOf<Constructor<GroupingPhaseInputsMerger.Worker>>()
    private val steps = mutableListOf<TestStepBuilder<*, *, TestStep.GroupingPhaseStep<*, *>>>()

    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractGroupingPhaseTestFacade<I, O>>,
    ): TestStepBuilder.FacadeStepBuilder.GroupingPhase<I, O> {
        return TestStepBuilder.FacadeStepBuilder.GroupingPhase(facade).also { steps += it }
    }

    fun <InputArtifact, InputArtifactKind> handlersStep(
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.GroupingPhase<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.GroupingPhase<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        return TestStepBuilder.HandlersStepBuilder.GroupingPhase(artifactKind, compilationStage).also {
            it.init()
            steps += it
        }
    }

    fun withMergerWorker(worker: Constructor<GroupingPhaseInputsMerger.Worker>) {
        mergerWorkers += worker
    }

    override fun build(testDataPath: String): GroupingPhaseTestConfiguration {
        val rootDisposable = TestDisposable("grouping-root")
        val testServices = TestServices()
        val defaultsProvider = defaultsProviderBuilder.build()
        val defaultRegisteredDirectives = defaultRegisteredDirectivesBuilder.build()
        val envConfigurators = environmentConfigurators.map { it(testServices) }

        testServices.register(
            org.cangnova.cangjie.test.services.TemporaryDirectoryManager::class,
            org.cangnova.cangjie.test.services.impl.TemporaryDirectoryManagerImpl(testServices)
        )

        testServices.register(AssertionsService::class, assertions)
        testServices.register(org.cangnova.cangjie.test.services.DefaultsProvider::class, defaultsProvider)
        testServices.register(org.cangnova.cangjie.test.services.EnvironmentConfiguratorsProvider::class, org.cangnova.cangjie.test.services.EnvironmentConfiguratorsProvider(envConfigurators))
        val sourceProvider = org.cangnova.cangjie.test.services.SourceFileProviderImpl(testServices, sourcePreprocessors.map { it(testServices) })
        testServices.register(org.cangnova.cangjie.test.services.SourceFileProvider::class, sourceProvider)
        val configurationProvider = compilerConfigurationProvider?.invoke(testServices, rootDisposable, envConfigurators)
            ?: org.cangnova.cangjie.test.services.CompilerConfigurationProviderImpl(testServices, rootDisposable, envConfigurators)
        testServices.register(org.cangnova.cangjie.test.services.CompilerConfigurationProvider::class, configurationProvider)
        testServices.register(org.cangnova.cangjie.test.services.DefaultRegisteredDirectivesProvider::class, org.cangnova.cangjie.test.services.DefaultRegisteredDirectivesProvider(defaultRegisteredDirectives))
        val runtimeProviders = runtimeClasspathProviders.map { it(testServices) }
        testServices.register(org.cangnova.cangjie.test.services.RuntimeClasspathProvidersContainer::class, org.cangnova.cangjie.test.services.RuntimeClasspathProvidersContainer(runtimeProviders))
        val processors = additionalMetaInfoProcessors.map { it(testServices) }
        testServices.register(org.cangnova.cangjie.test.services.GlobalMetadataInfoHandler::class, org.cangnova.cangjie.test.services.GlobalMetadataInfoHandler(testServices, processors))
        additionalServices.forEach { testServices.register(it, skipAlreadyRegistered = false) }

        val allDirectives = directives.toMutableList()
        envConfigurators.forEach { registerServicesAndDirectives(allDirectives, testServices, it) }
        val metaConfigurators = metaTestConfigurators.map { it(testServices) }.onEach { registerServicesAndDirectives(allDirectives, testServices, it) }
        val checkers = (afterAnalysisCheckers + ::UpdateTestDataHandler).map { it(testServices) }.onEach {
            registerServicesAndDirectives(allDirectives, testServices, it)
        }.sortedBy { it.order }

        val createdSteps = steps.map { it.createTestStep(testServices) }

        return GroupingPhaseTestConfigurationImpl(
            rootDisposable = rootDisposable,
            testServices = testServices,
            directives = when (allDirectives.size) {
                0 -> DirectivesContainer.Empty
                1 -> allDirectives.single()
                else -> ComposedDirectivesContainer(allDirectives)
            },
            defaultRegisteredDirectives = defaultRegisteredDirectives,
            moduleStructureExtractor = ModuleStructureExtractorImpl(
                testServices = testServices,
                additionalSourceProviders = emptyList(),
                moduleStructureTransformers = emptyList(),
                environmentConfigurators = envConfigurators
            ),
            preAnalysisHandlers = preAnalysisHandlers.map { it(testServices) },
            metaTestConfigurators = metaConfigurators,
            afterAnalysisCheckers = checkers,
            metaInfoHandlerEnabled = metaInfoHandlerEnabled,
            steps = createdSteps,
            mergerWorkers = mergerWorkers.map { it(testServices) },
        )
    }

    private fun registerServicesAndDirectives(
        allDirectives: MutableList<DirectivesContainer>,
        testServices: TestServices,
        container: ServicesAndDirectivesContainer,
    ) {
        allDirectives += container.directiveContainers
        testServices.register(container.additionalServices, skipAlreadyRegistered = true)
    }
}

typealias TestConfigurationBuilder = NonGroupingPhaseTestConfigurationBuilder

@DefaultsDsl
class TwoPhaseTestConfigurationBuilder {
    val firstPhaseBuilder = NonGroupingPhaseTestConfigurationBuilder()
    val secondPhaseBuilder = GroupingPhaseTestConfigurationBuilder()

    fun commonConfiguration(init: TestConfigurationBuilderBase<*, *>.() -> Unit) {
        firstPhaseBuilder.apply(init)
        secondPhaseBuilder.apply(init)
    }

    fun nonGroupingPhase(init: NonGroupingPhaseTestConfigurationBuilder.() -> Unit) {
        firstPhaseBuilder.apply(init)
    }

    fun groupingPhase(init: GroupingPhaseTestConfigurationBuilder.() -> Unit) {
        secondPhaseBuilder.apply(init)
    }
}

inline fun testConfiguration(
    testDataPath: String,
    init: NonGroupingPhaseTestConfigurationBuilder.() -> Unit,
): NonGroupingPhaseTestConfiguration {
    return NonGroupingPhaseTestConfigurationBuilder().apply(init).build(testDataPath)
}
