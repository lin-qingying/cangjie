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
import org.cangnova.cangjie.test.services.impl.ModuleStructureExtractorImpl
import org.cangnova.cangjie.test.services.ModuleStructureTransformer
import org.cangnova.cangjie.test.services.PreAnalysisHandler
import org.cangnova.cangjie.test.services.RuntimeClasspathProvider
import org.cangnova.cangjie.test.services.ServiceRegistrationData
import org.cangnova.cangjie.test.services.SourceFilePreprocessor
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.service
import java.nio.file.Path

/**
 * 表示 `TestConfigurationBuilderBase`，承载测试配置构建中的配置数据、测试产物或处理步骤。
 */
@DefaultsDsl
@OptIn(TestInfrastructureInternals::class)
abstract class TestConfigurationBuilderBase<Self : TestConfigurationBuilderBase<Self, C>, C> {
    /**
     * 保存 `defaultsProviderBuilder`，供测试配置构建在测试执行期间读取或传递。
     */
    val defaultsProviderBuilder: DefaultsProviderBuilder = DefaultsProviderBuilder()
    /**
     * 保存 `assertions`，供测试配置构建在测试执行期间读取或传递。
     */
    lateinit var assertions: AssertionsService

    /**
     * 保存 `sourcePreprocessors`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val sourcePreprocessors = mutableListOf<Constructor<SourceFilePreprocessor>>()
    /**
     * 保存 `additionalMetaInfoProcessors`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val additionalMetaInfoProcessors = mutableListOf<Constructor<AdditionalMetaInfoProcessor>>()
    /**
     * 保存 `environmentConfigurators`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val environmentConfigurators = mutableListOf<Constructor<AbstractEnvironmentConfigurator>>()
    /**
     * 保存 `preAnalysisHandlers`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val preAnalysisHandlers = mutableListOf<Constructor<PreAnalysisHandler>>()

    /**
     * 保存 `additionalSourceProviders`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val additionalSourceProviders = mutableListOf<Constructor<AdditionalSourceProvider>>()
    /**
     * 保存 `moduleStructureTransformers`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val moduleStructureTransformers = mutableListOf<Constructor<ModuleStructureTransformer>>()

    /**
     * 保存 `metaTestConfigurators`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val metaTestConfigurators = mutableListOf<Constructor<org.cangnova.cangjie.test.services.MetaTestConfigurator>>()
    /**
     * 保存 `afterAnalysisCheckers`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val afterAnalysisCheckers = mutableListOf<Constructor<AfterAnalysisChecker>>()

    /**
     * 保存 `metaInfoHandlerEnabled`，供测试配置构建在测试执行期间读取或传递。
     */
    protected var metaInfoHandlerEnabled: Boolean = false

    /**
     * 保存 `directives`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val directives = mutableListOf<DirectivesContainer>()
    /**
     * 保存 `defaultRegisteredDirectivesBuilder`，供测试配置构建在测试执行期间读取或传递。
     */
    val defaultRegisteredDirectivesBuilder = RegisteredDirectivesBuilder()

    /**
     * 保存 `additionalServices`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val additionalServices = mutableListOf<ServiceRegistrationData>()

    /**
     * 保存 `compilerConfigurationProvider`，供测试配置构建在测试执行期间读取或传递。
     */
    protected var compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)? =
        null
    /**
     * 保存 `runtimeClasspathProviders`，供测试配置构建在测试执行期间读取或传递。
     */
    protected val runtimeClasspathProviders = mutableListOf<Constructor<RuntimeClasspathProvider>>()

    /**
     * 提供 `useAdditionalService` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    inline fun <reified T : TestService> useAdditionalService(noinline serviceConstructor: (TestServices) -> T) {
        useAdditionalServices(service(serviceConstructor))
    }

    /**
     * 执行 `useAdditionalServices` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useAdditionalServices(vararg serviceRegistrationData: ServiceRegistrationData) {
        additionalServices += serviceRegistrationData
    }

    /**
     * 提供 `globalDefaults` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    open fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        defaultsProviderBuilder.apply(init)
    }

    /**
     * 执行 `useSourcePreprocessor` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useSourcePreprocessor(vararg preprocessors: Constructor<SourceFilePreprocessor>, needToPrepend: Boolean = false) {
        if (needToPrepend) {
            sourcePreprocessors.addAll(0, preprocessors.toList())
        } else {
            sourcePreprocessors.addAll(preprocessors)
        }
    }

    /**
     * 执行 `useDirectives` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useDirectives(vararg directives: DirectivesContainer) {
        this.directives += directives
    }

    /**
     * 执行 `useConfigurators` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useConfigurators(vararg environmentConfigurators: Constructor<AbstractEnvironmentConfigurator>) {
        this.environmentConfigurators += environmentConfigurators
    }

    /**
     * 执行 `usePreAnalysisHandlers` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun usePreAnalysisHandlers(vararg handlers: Constructor<PreAnalysisHandler>) {
        this.preAnalysisHandlers += handlers
    }

    /**
     * 执行 `useMetaInfoProcessors` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useMetaInfoProcessors(vararg processors: Constructor<AdditionalMetaInfoProcessor>) {
        additionalMetaInfoProcessors += processors
    }

    /**
     * 执行 `useAdditionalSourceProviders` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useAdditionalSourceProviders(vararg providers: Constructor<AdditionalSourceProvider>) {
        additionalSourceProviders += providers
    }

    /**
     * 执行 `useModuleStructureTransformers` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    @TestInfrastructureInternals
    fun useModuleStructureTransformers(vararg transformers: Constructor<ModuleStructureTransformer>) {
        moduleStructureTransformers += transformers
    }

    /**
     * 执行 `useCustomCompilerConfigurationProvider` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    @TestInfrastructureInternals
    fun useCustomCompilerConfigurationProvider(provider: (TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider) {
        compilerConfigurationProvider = provider
    }

    /**
     * 执行 `useCustomRuntimeClasspathProviders` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useCustomRuntimeClasspathProviders(vararg provider: Constructor<RuntimeClasspathProvider>) {
        runtimeClasspathProviders += provider
    }

    /**
     * 执行 `useMetaTestConfigurators` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useMetaTestConfigurators(vararg configurators: Constructor<org.cangnova.cangjie.test.services.MetaTestConfigurator>) {
        metaTestConfigurators += configurators
    }

    /**
     * 执行 `useAfterAnalysisCheckers` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun useAfterAnalysisCheckers(vararg checkers: Constructor<AfterAnalysisChecker>, insertAtFirst: Boolean = false) {
        if (insertAtFirst) {
            afterAnalysisCheckers.addAll(0, checkers.toList())
        } else {
            afterAnalysisCheckers += checkers
        }
    }

    /**
     * 提供 `defaultDirectives` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    open fun defaultDirectives(init: RegisteredDirectivesBuilder.() -> Unit) {
        defaultRegisteredDirectivesBuilder.apply(init)
    }

    /**
     * 提供 `build` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    abstract fun build(testDataPath: String): C
}

/**
 * 表示 `NonGroupingPhaseTestConfigurationBuilder`，承载测试配置构建中的配置数据、测试产物或处理步骤。
 */
@DefaultsDsl
@OptIn(TestInfrastructureInternals::class)
class NonGroupingPhaseTestConfigurationBuilder :
    TestConfigurationBuilderBase<NonGroupingPhaseTestConfigurationBuilder, NonGroupingPhaseTestConfiguration>() {
    /**
     * 保存 `testInfo`，供测试配置构建在测试执行期间读取或传递。
     */
    lateinit var testInfo: CangJieTestInfo
    /**
     * 维护 `startingArtifactFactory`，供测试配置构建在测试执行期间读取或传递。
     */
    var startingArtifactFactory: (TestModule) -> ResultingArtifact<*> = { ResultingArtifact.Source() }

    /**
     * 保存 `steps`，供测试配置构建在测试执行期间读取或传递。
     */
    private val steps = mutableListOf<TestStepBuilder<*, *, TestStep.NonGroupingStep<*, *>>>()
    /**
     * 保存 `namedSteps`，供测试配置构建在测试执行期间读取或传递。
     */
    private val namedSteps = mutableMapOf<String, TestStepBuilder.HandlersStepBuilder.NonGroupingPhase<*, *>>()

    /**
     * 保存 `globalDefaultsConfigurators`，供测试配置构建在测试执行期间读取或传递。
     */
    private val globalDefaultsConfigurators = mutableListOf<DefaultsProviderBuilder.() -> Unit>()
    /**
     * 保存 `defaultDirectiveConfigurators`，供测试配置构建在测试执行期间读取或传递。
     */
    private val defaultDirectiveConfigurators = mutableListOf<RegisteredDirectivesBuilder.() -> Unit>()
    /**
     * 保存 `configurationsByPositiveTestDataCondition`，供测试配置构建在测试执行期间读取或传递。
     */
    private val configurationsByPositiveTestDataCondition = mutableListOf<Pair<Regex, NonGroupingPhaseTestConfigurationBuilder.() -> Unit>>()
    /**
     * 保存 `configurationsByNegativeTestDataCondition`，供测试配置构建在测试执行期间读取或传递。
     */
    private val configurationsByNegativeTestDataCondition = mutableListOf<Pair<Regex, NonGroupingPhaseTestConfigurationBuilder.() -> Unit>>()

    /**
     * 执行 `forTestsMatching` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun forTestsMatching(pattern: String, configuration: NonGroupingPhaseTestConfigurationBuilder.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        configurationsByPositiveTestDataCondition += regex to configuration
    }

    /**
     * 执行 `forTestsNotMatching` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun forTestsNotMatching(pattern: String, configuration: NonGroupingPhaseTestConfigurationBuilder.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        configurationsByNegativeTestDataCondition += regex to configuration
    }

    /**
     * 提供 `or` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    infix fun String.or(other: String): String = "$this|$other"

    /**
     * 提供 `toMatchingRegexString` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    private fun String.toMatchingRegexString(): String = when (this) {
        "*" -> ".*"
        else -> "^.*/(${replace("*", ".*")})$"
    }

    /**
     * 执行 `globalDefaults` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    override fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        globalDefaultsConfigurators += init
        super.globalDefaults(init)
    }

    /**
     * 执行 `defaultDirectives` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    override fun defaultDirectives(init: RegisteredDirectivesBuilder.() -> Unit) {
        defaultDirectiveConfigurators += init
        super.defaultDirectives(init)
    }

    /**
     * 执行 `,` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractTestFacade<I, O>>,
    ): TestStepBuilder.FacadeStepBuilder.NonGroupingPhase<I, O> {
        return TestStepBuilder.FacadeStepBuilder.NonGroupingPhase(facade).also { steps += it }
    }

    /**
     * 执行 `handlersStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `namedHandlersStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `configureNamedHandlersStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `enableMetaInfoHandler` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun enableMetaInfoHandler() {
        metaInfoHandlerEnabled = true
    }

    /**
     * 执行 `build` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
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

    /**
     * 表示 `ReadOnlyBuilder`，承载测试配置构建中的配置数据、测试产物或处理步骤。
     */
    class ReadOnlyBuilder(private val builder: NonGroupingPhaseTestConfigurationBuilder, val testDataPath: String) {
        /**
         * 保存 `defaultsProviderBuilder`，供测试配置构建在测试执行期间读取或传递。
         */
        val defaultsProviderBuilder: DefaultsProviderBuilder
            get() = builder.defaultsProviderBuilder
        /**
         * 保存 `assertions`，供测试配置构建在测试执行期间读取或传递。
         */
        val assertions: AssertionsService
            get() = builder.assertions
        /**
         * 保存 `sourcePreprocessors`，供测试配置构建在测试执行期间读取或传递。
         */
        val sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>
            get() = builder.sourcePreprocessors
        /**
         * 保存 `additionalMetaInfoProcessors`，供测试配置构建在测试执行期间读取或传递。
         */
        val additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>
            get() = builder.additionalMetaInfoProcessors
        /**
         * 保存 `environmentConfigurators`，供测试配置构建在测试执行期间读取或传递。
         */
        val environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>
            get() = builder.environmentConfigurators
        /**
         * 保存 `preAnalysisHandlers`，供测试配置构建在测试执行期间读取或传递。
         */
        val preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>
            get() = builder.preAnalysisHandlers
        /**
         * 保存 `additionalSourceProviders`，供测试配置构建在测试执行期间读取或传递。
         */
        val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>
            get() = builder.additionalSourceProviders
        /**
         * 保存 `moduleStructureTransformers`，供测试配置构建在测试执行期间读取或传递。
         */
        val moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>
            get() = builder.moduleStructureTransformers
        /**
         * 保存 `metaTestConfigurators`，供测试配置构建在测试执行期间读取或传递。
         */
        val metaTestConfigurators: List<Constructor<org.cangnova.cangjie.test.services.MetaTestConfigurator>>
            get() = builder.metaTestConfigurators
        /**
         * 保存 `afterAnalysisCheckers`，供测试配置构建在测试执行期间读取或传递。
         */
        val afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
            get() = builder.afterAnalysisCheckers
        /**
         * 保存 `metaInfoHandlerEnabled`，供测试配置构建在测试执行期间读取或传递。
         */
        val metaInfoHandlerEnabled: Boolean
            get() = builder.metaInfoHandlerEnabled
        /**
         * 保存 `directives`，供测试配置构建在测试执行期间读取或传递。
         */
        val directives: List<DirectivesContainer>
            get() = builder.directives
        /**
         * 保存 `defaultDirectiveConfigurators`，供测试配置构建在测试执行期间读取或传递。
         */
        val defaultDirectiveConfigurators: List<RegisteredDirectivesBuilder.() -> Unit>
            get() = builder.defaultDirectiveConfigurators
        /**
         * 保存 `globalDefaultsConfigurators`，供测试配置构建在测试执行期间读取或传递。
         */
        val globalDefaultsConfigurators: List<DefaultsProviderBuilder.() -> Unit>
            get() = builder.globalDefaultsConfigurators
        /**
         * 保存 `configurationsByPositiveTestDataCondition`，供测试配置构建在测试执行期间读取或传递。
         */
        val configurationsByPositiveTestDataCondition: List<Pair<Regex, NonGroupingPhaseTestConfigurationBuilder.() -> Unit>>
            get() = builder.configurationsByPositiveTestDataCondition
        /**
         * 保存 `configurationsByNegativeTestDataCondition`，供测试配置构建在测试执行期间读取或传递。
         */
        val configurationsByNegativeTestDataCondition: List<Pair<Regex, NonGroupingPhaseTestConfigurationBuilder.() -> Unit>>
            get() = builder.configurationsByNegativeTestDataCondition
        /**
         * 保存 `additionalServices`，供测试配置构建在测试执行期间读取或传递。
         */
        val additionalServices: List<ServiceRegistrationData>
            get() = builder.additionalServices
        /**
         * 保存 `compilerConfigurationProvider`，供测试配置构建在测试执行期间读取或传递。
         */
        val compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?
            get() = builder.compilerConfigurationProvider
        /**
         * 保存 `runtimeClasspathProviders`，供测试配置构建在测试执行期间读取或传递。
         */
        val runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>
            get() = builder.runtimeClasspathProviders
        /**
         * 保存 `testInfo`，供测试配置构建在测试执行期间读取或传递。
         */
        val testInfo: CangJieTestInfo
            get() = builder.testInfo
        /**
         * 保存 `startingArtifactFactory`，供测试配置构建在测试执行期间读取或传递。
         */
        val startingArtifactFactory: (TestModule) -> ResultingArtifact<*>
            get() = builder.startingArtifactFactory
    }
}

/**
 * 表示 `GroupingPhaseTestConfigurationBuilder`，承载测试配置构建中的配置数据、测试产物或处理步骤。
 */
@DefaultsDsl
@OptIn(TestInfrastructureInternals::class)
class GroupingPhaseTestConfigurationBuilder :
    TestConfigurationBuilderBase<GroupingPhaseTestConfigurationBuilder, GroupingPhaseTestConfiguration>() {
    /**
     * 保存 `testInfo`，供测试配置构建在测试执行期间读取或传递。
     */
    lateinit var testInfo: CangJieTestInfo
    /**
     * 保存 `mergerWorkers`，供测试配置构建在测试执行期间读取或传递。
     */
    private val mergerWorkers = mutableListOf<Constructor<GroupingPhaseInputsMerger.Worker>>()
    /**
     * 保存 `steps`，供测试配置构建在测试执行期间读取或传递。
     */
    private val steps = mutableListOf<TestStepBuilder<*, *, TestStep.GroupingPhaseStep<*, *>>>()

    /**
     * 执行 `,` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractGroupingPhaseTestFacade<I, O>>,
    ): TestStepBuilder.FacadeStepBuilder.GroupingPhase<I, O> {
        return TestStepBuilder.FacadeStepBuilder.GroupingPhase(facade).also { steps += it }
    }

    /**
     * 执行 `handlersStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `withMergerWorker` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun withMergerWorker(worker: Constructor<GroupingPhaseInputsMerger.Worker>) {
        mergerWorkers += worker
    }

    /**
     * 执行 `build` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `registerServicesAndDirectives` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    private fun registerServicesAndDirectives(
        allDirectives: MutableList<DirectivesContainer>,
        testServices: TestServices,
        container: ServicesAndDirectivesContainer,
    ) {
        allDirectives += container.directiveContainers
        testServices.register(container.additionalServices, skipAlreadyRegistered = true)
    }
}

/**
 * 定义 `TestConfigurationBuilder` 类型别名，统一测试配置构建中的回调或构造签名。
 */
typealias TestConfigurationBuilder = NonGroupingPhaseTestConfigurationBuilder

/**
 * 表示 `TwoPhaseTestConfigurationBuilder`，承载测试配置构建中的配置数据、测试产物或处理步骤。
 */
@DefaultsDsl
class TwoPhaseTestConfigurationBuilder {
    /**
     * 保存 `firstPhaseBuilder`，供测试配置构建在测试执行期间读取或传递。
     */
    val firstPhaseBuilder = NonGroupingPhaseTestConfigurationBuilder()
    /**
     * 保存 `secondPhaseBuilder`，供测试配置构建在测试执行期间读取或传递。
     */
    val secondPhaseBuilder = GroupingPhaseTestConfigurationBuilder()

    /**
     * 执行 `commonConfiguration` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun commonConfiguration(init: TestConfigurationBuilderBase<*, *>.() -> Unit) {
        firstPhaseBuilder.apply(init)
        secondPhaseBuilder.apply(init)
    }

    /**
     * 执行 `nonGroupingPhase` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun nonGroupingPhase(init: NonGroupingPhaseTestConfigurationBuilder.() -> Unit) {
        firstPhaseBuilder.apply(init)
    }

    /**
     * 执行 `groupingPhase` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun groupingPhase(init: GroupingPhaseTestConfigurationBuilder.() -> Unit) {
        secondPhaseBuilder.apply(init)
    }
}

/**
 * 提供 `testConfiguration` 对应的测试配置构建流程，维持测试框架的阶段契约。
 */
inline fun testConfiguration(
    testDataPath: String,
    init: NonGroupingPhaseTestConfigurationBuilder.() -> Unit,
): NonGroupingPhaseTestConfiguration {
    return NonGroupingPhaseTestConfigurationBuilder().apply(init).build(testDataPath)
}
