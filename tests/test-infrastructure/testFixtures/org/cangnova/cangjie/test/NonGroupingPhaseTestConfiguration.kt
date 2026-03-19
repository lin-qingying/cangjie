package org.cangnova.cangjie.test

import com.intellij.openapi.Disposable
import org.cangnova.cangjie.test.builders.NonGroupingPhaseTestConfigurationBuilder
import org.cangnova.cangjie.test.builders.TestStepBuilder
import org.cangnova.cangjie.test.directives.model.ComposedDirectivesContainer
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.ServicesAndDirectivesContainer
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.AbstractEnvironmentConfigurator
import org.cangnova.cangjie.test.services.AdditionalMetaInfoProcessor
import org.cangnova.cangjie.test.services.AdditionalSourceProvider
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.test.services.CompilerConfigurationProvider
import org.cangnova.cangjie.test.services.CompilerConfigurationProviderImpl
import org.cangnova.cangjie.test.services.DefaultRegisteredDirectivesProvider
import org.cangnova.cangjie.test.services.DefaultsProvider
import org.cangnova.cangjie.test.services.EnvironmentConfiguratorsProvider
import org.cangnova.cangjie.test.services.GlobalMetadataInfoHandler
import org.cangnova.cangjie.test.services.MetaTestConfigurator
import org.cangnova.cangjie.test.services.ModuleStructureExtractor
import org.cangnova.cangjie.test.services.impl.ModuleStructureExtractorImpl
import org.cangnova.cangjie.test.services.ModuleStructureTransformer
import org.cangnova.cangjie.test.services.PreAnalysisHandler
import org.cangnova.cangjie.test.services.RuntimeClasspathProvider
import org.cangnova.cangjie.test.services.RuntimeClasspathProvidersContainer
import org.cangnova.cangjie.test.services.ServiceRegistrationData
import org.cangnova.cangjie.test.services.SourceFilePreprocessor
import org.cangnova.cangjie.test.services.SourceFileProvider
import org.cangnova.cangjie.test.services.SourceFileProviderImpl
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

interface NonGroupingPhaseTestConfiguration : TestConfiguration<TestStep.NonGroupingStep<*, *>> {
    val startingArtifactFactory: (TestModule) -> ResultingArtifact<*>
}

@OptIn(TestInfrastructureInternals::class)
class NonGroupingPhaseTestConfigurationImpl(
    testInfo: CangJieTestInfo,
    defaultsProvider: DefaultsProvider,
    assertions: AssertionsService,
    steps: List<TestStepBuilder<*, *, TestStep.NonGroupingStep<*, *>>>,
    sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>,
    additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>,
    environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>,
    additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>,
    preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>,
    moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>,
    metaTestConfigurators: List<Constructor<MetaTestConfigurator>>,
    afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>,
    compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?,
    runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>,
    metaInfoHandlerEnabled: Boolean,
    directives: List<DirectivesContainer>,
    defaultRegisteredDirectives: RegisteredDirectives,
    override var startingArtifactFactory: (TestModule) -> ResultingArtifact<*>,
    additionalServices: List<ServiceRegistrationData>,
    val originalBuilder: NonGroupingPhaseTestConfigurationBuilder.ReadOnlyBuilder,
) : TestConfigurationImplBase<TestStep.NonGroupingStep<*, *>>(
    testInfo, defaultsProvider, assertions, steps, sourcePreprocessors, additionalMetaInfoProcessors, environmentConfigurators,
    additionalSourceProviders, preAnalysisHandlers, moduleStructureTransformers, metaTestConfigurators, afterAnalysisCheckers,
    compilerConfigurationProvider, runtimeClasspathProviders, metaInfoHandlerEnabled, directives, defaultRegisteredDirectives,
    additionalServices
), NonGroupingPhaseTestConfiguration


@OptIn(TestInfrastructureInternals::class)
sealed class TestConfigurationImplBase<Step : TestStep<*, *>>(
    testInfo: CangJieTestInfo,

    defaultsProvider: DefaultsProvider,
    assertions: AssertionsService,

    steps: List<TestStepBuilder<*, *, Step>>,

    sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>,
    additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>,
    environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>,

    additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>,
    preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>,
    moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>,
    metaTestConfigurators: List<Constructor<MetaTestConfigurator>>,
    afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>,

    compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?,
    runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>,

    override val metaInfoHandlerEnabled: Boolean,

    directives: List<DirectivesContainer>,
    override val defaultRegisteredDirectives: RegisteredDirectives,
    additionalServices: List<ServiceRegistrationData>,
) : TestConfiguration<Step>, TestService {
    override val rootDisposable: Disposable = TestDisposable("${this::class.simpleName}.rootDisposable")
    override val testServices: TestServices = TestServices()

    init {
        testServices.register(TestConfigurationImplBase::class, this)
        testServices.register(CangJieTestInfo::class, testInfo)
        val runtimeClassPathProviders = runtimeClasspathProviders.map { it.invoke(testServices) }
        testServices.register(RuntimeClasspathProvidersContainer::class, RuntimeClasspathProvidersContainer(runtimeClassPathProviders))
        additionalServices.forEach {
            testServices.register(it, skipAlreadyRegistered = false)
        }
    }

    private val allDirectives = directives.toMutableSet()
    override val directives: DirectivesContainer by lazy {
        when (allDirectives.size) {
            0 -> DirectivesContainer.Empty
            1 -> allDirectives.single()
            else -> ComposedDirectivesContainer(allDirectives)
        }
    }

    private val environmentConfigurators: List<AbstractEnvironmentConfigurator> =
        environmentConfigurators
            .map { it.invoke(testServices) }
            .also { it.registerDirectivesAndServices() }

    override val preAnalysisHandlers: List<PreAnalysisHandler> =
        preAnalysisHandlers.map { it.invoke(testServices) }

    override val moduleStructureExtractor: ModuleStructureExtractor = ModuleStructureExtractorImpl(
        testServices,
        additionalSourceProviders
            .map { it.invoke(testServices) }
            .also { it.registerDirectivesAndServices() },
        moduleStructureTransformers.map { it(testServices) },
        this.environmentConfigurators
    )

    override val metaTestConfigurators: List<MetaTestConfigurator> = metaTestConfigurators.map { constructor ->
        constructor.invoke(testServices).also { it.registerDirectivesAndServices() }
    }

    init {
        testServices.apply {
            register(
                EnvironmentConfiguratorsProvider::class,
                EnvironmentConfiguratorsProvider(this@TestConfigurationImplBase.environmentConfigurators)
            )
            val sourceFilePreprocessors = sourcePreprocessors.map { it.invoke(this@apply) }
            val sourceFileProvider = SourceFileProviderImpl(this, sourceFilePreprocessors)
            register(SourceFileProvider::class, sourceFileProvider)

            val environmentProvider =
                compilerConfigurationProvider?.invoke(this, rootDisposable, this@TestConfigurationImplBase.environmentConfigurators)
                    ?: CompilerConfigurationProviderImpl(
                        this,
                        rootDisposable,
                        this@TestConfigurationImplBase.environmentConfigurators
                    )
            register(CompilerConfigurationProvider::class, environmentProvider)

            register(AssertionsService::class, assertions)
            register(DefaultsProvider::class, defaultsProvider)

            register(DefaultRegisteredDirectivesProvider::class, DefaultRegisteredDirectivesProvider(defaultRegisteredDirectives))

            val metaInfoProcessors = additionalMetaInfoProcessors.map { it.invoke(this) }
            register(GlobalMetadataInfoHandler::class, GlobalMetadataInfoHandler(this, metaInfoProcessors))
        }
    }

    final override val steps: List<Step>
    final override val afterAnalysisCheckers: List<AfterAnalysisChecker>

    init {
        val afterAnalysisCheckerConstructors = mutableSetOf<Constructor<AfterAnalysisChecker>>()

        this.steps = steps
            .map { it.createTestStep(testServices) }
            .onEach { step ->
                when (step) {
                    is TestStep.FacadeStep<*, *> -> step.facade.registerDirectivesAndServices()
                    is TestStep.HandlersStep<*> -> {
                        step.handlers.registerDirectivesAndServices()
                        step.handlers.flatMapTo(afterAnalysisCheckerConstructors) { it.additionalAfterAnalysisCheckers }
                    }
                }
            }
        afterAnalysisCheckerConstructors.addAll(afterAnalysisCheckers)
        this.afterAnalysisCheckers = afterAnalysisCheckerConstructors.map { constructor ->
            constructor.invoke(testServices).also { it.registerDirectivesAndServices() }
        }.sortedBy { it.order }
    }

    private fun ServicesAndDirectivesContainer.registerDirectivesAndServices() {
        allDirectives += directiveContainers
        testServices.register(additionalServices, skipAlreadyRegistered = true)
    }

    private fun List<ServicesAndDirectivesContainer>.registerDirectivesAndServices() {
        this.forEach { it.registerDirectivesAndServices() }
    }
}
