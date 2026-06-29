package org.cangnova.cangjie.test

import com.intellij.testFramework.TestDataFile

import org.cangnova.cangjie.test.model.AnalysisHandler
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.*

/**
 * 非分组测试运行器
 *
 * 对应 Kotlin K2 的 NonGroupingTestRunner
 */
class NonGroupingTestRunner(
    testConfiguration: NonGroupingPhaseTestConfiguration
) : TestRunner<TestStep.NonGroupingStep<*, *>, NonGroupingPhaseTestConfiguration>(testConfiguration) {
    /**
     * 保存 `allRanHandlers`，供测试基础设施在测试执行期间读取或传递。
     */
    private val allRanHandlers = mutableSetOf<AnalysisHandler<*>>()

    /**
     * 执行 `runTest` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun runTest(@TestDataFile testDataFileName: String, beforeDispose: (NonGroupingPhaseTestConfiguration) -> Unit = {}) {
        try {
            prepareModuleStructure(testDataFileName)
            runTestPipeline()
        } finally {
            finalizeAndDispose(beforeDispose)
        }
    }

    /**
     * 执行 `prepareModuleStructure` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun prepareModuleStructure(testDataFileName: String): TestModuleStructure? {
        val services = testServices

        @Suppress("NAME_SHADOWING")
        val testDataFileName = testConfiguration.metaTestConfigurators.fold(testDataFileName) { fileName, configurator ->
            configurator.transformTestDataPath(fileName)
        }

        val moduleStructure = try {
            testConfiguration.moduleStructureExtractor.splitTestDataByModules(
                testDataFileName,
                testConfiguration.directives,
            ).also {
                services.register(TestModuleStructure::class, it)
            }
        } catch (e: ExceptionFromModuleStructureTransformer) {
            services.register(TestModuleStructure::class, e.alreadyParsedModuleStructure)
            val exception = filterFailedExceptions(
                listOf(WrappedException.FromModuleStructureTransformer(e.cause))
            ).firstOrNull() ?: return null
            throw exception
        }

        testConfiguration.metaTestConfigurators.forEach {
            services.assertions.assumeFalse(it.shouldSkipTest()) { "Test skipped by ${it::class.simpleName}" }
        }
        return moduleStructure
    }
    /**
     * 执行 `runTestPipeline` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun runTestPipeline() {
        runTestPreprocessing()
        runSteps()
        reportFailures()
    }
    /**
     * 执行 `runTestPreprocessing` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun runTestPreprocessing() {
        super.runTestPreprocessing()
        val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler
        globalMetadataInfoHandler.parseExistingMetadataInfosFromAllSources()
    }
    /**
     * 执行 `runSteps` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun runSteps() {
        val services = testConfiguration.testServices
        val moduleStructure = services.moduleStructure

        for (module in moduleStructure.modules) {
            val shouldProcessNextModules = processModule(module, services.artifactsProvider)
            if (!shouldProcessNextModules) break
        }

        for (handler in allRanHandlers) {
            val wrapperFactory: (Throwable) -> WrappedException = { WrappedException.FromHandler(it, failedModule = null, handler) }
            withAssertionCatching(wrapperFactory) {
                val thereWasAnException = allFailedExceptions.isNotEmpty()
                if (handler.shouldRun(thereWasAnException)) {
                    handler.processAfterAllModules(thereWasAnException)
                }
            }
        }

        if (testConfiguration.metaInfoHandlerEnabled) {
            withAssertionCatching(WrappedException::FromMetaInfoHandler) {
                services.globalMetadataInfoHandler.compareAllMetaDataInfos()
            }
        }

        testConfiguration.afterAnalysisCheckers.forEach {
            withAssertionCatching(WrappedException::FromAfterAnalysisChecker) {
                it.check(allFailedExceptions)
            }
        }
    }


    /**
     * 提供 `processModule` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun processModule(
        module: TestModule,
        artifactsProvider: ArtifactsProvider
    ): Boolean {
        return runPipelineOnSingleUnit(
            produceStartingArtifact = { testConfiguration.startingArtifactFactory(module) },
            shouldRunStep = { step, inputArtifact -> step.shouldProcessModule(module, inputArtifact) },
            runStep =   { step, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps ->
                step.hackyProcessModule(module, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps)
            },
            onArtifactResult = { artifactsProvider.registerArtifact(module, it) },
            onHandlersResult = { step ->
                require(step is TestStep.HandlersStep<*>)
                allRanHandlers += step.handlers.filterIsInstance<AnalysisHandler<*>>()
            }
        )
    }

    /**
     * 提供 `NonGroupingStep` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun TestStep.NonGroupingStep<*, *>.hackyProcessModule(
        module: TestModule,
        inputArtifact: ResultingArtifact<*>,
        thereWereExceptionsOnPreviousSteps: Boolean
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return (this as TestStep.NonGroupingStep<ResultingArtifact.Source, *>)
            .processModule(module, inputArtifact as ResultingArtifact<ResultingArtifact.Source>, thereWereExceptionsOnPreviousSteps)
    }

    /**
     * 提供 `>` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun <I : ResultingArtifact<I>> TestStep.NonGroupingStep<I, *>.processModule(
        module: TestModule,
        artifact: ResultingArtifact<I>,
        thereWereExceptionsOnPreviousSteps: Boolean
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return processModule(module, artifact as I, thereWereExceptionsOnPreviousSteps)
    }
}
