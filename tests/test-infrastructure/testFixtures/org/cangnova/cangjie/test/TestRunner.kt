package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.services.*
import org.cangnova.cangjie.utils.firstIsInstanceOrNull
import java.io.IOException

/**
 * 测试运行器
 *
 * 对应 Kotlin K2 的 TestRunner
 */
sealed class

TestRunner<Step : TestStep<*, *>, Configuration : TestConfiguration<Step>>(val testConfiguration: Configuration) {
    /**
     * 保存 `testServices`，供测试基础设施在测试执行期间读取或传递。
     */
    val testServices: TestServices get() = testConfiguration.testServices
    /**
     * 保存 `allFailedExceptions`，供测试基础设施在测试执行期间读取或传递。
     */
    protected val allFailedExceptions = mutableListOf<WrappedException>()

    /**
     * 提供 `runTestPreprocessing` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    open fun runTestPreprocessing() {
        val moduleStructure = testServices.moduleStructure

        val modules = moduleStructure.modules
        val artifactsProvider = ArtifactsProvider(testServices, modules)
        testServices.registerArtifactsProvider(artifactsProvider)

        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            preprocessor.preprocessModuleStructure(moduleStructure)
        }

        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            withAssertionCatching(WrappedException::FromPreAnalysisHandler) {
                preprocessor.prepareSealedClassInheritors(moduleStructure)
            }
        }
    }

    /**
     * 执行 `reportFailures` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun reportFailures() {
        val filteredFailedAssertions = filterFailedExceptions(allFailedExceptions)
        filteredFailedAssertions.firstIsInstanceOrNull<WrappedException.FromFacade>()?.let {
            throw it
        }
        testServices.assertions.failAll(filteredFailedAssertions)
    }

    /**
     * 执行 `finalizeAndDispose` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun finalizeAndDispose(beforeDispose: (Configuration) -> Unit = {}) {
        try {
            testConfiguration.testServices.temporaryDirectoryManager.cleanupTemporaryDirectories()
        } catch (e: IOException) {
            println("Failed to clean temporary directories:")
            e.printStackTrace()
        }
        beforeDispose(testConfiguration)
        // TODO: disposeRootInWriteAction(testConfiguration.rootDisposable)
    }

    /**
     * 提供 `interface` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected fun interface RunStep<Step : TestStep<*, *>> {
        fun run(
            step: Step,
            inputArtifact: ResultingArtifact<*>,
            thereWereCriticalExceptionsOnPreviousSteps: Boolean
        ): TestStep.StepResult<*>
    }

    /**
     * 提供 `runPipelineOnSingleUnit` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected fun runPipelineOnSingleUnit(
        produceStartingArtifact: () -> ResultingArtifact<*>,
        shouldRunStep: (Step, ResultingArtifact<*>) -> Boolean,
        runStep: RunStep<Step>,
        onArtifactResult: (ResultingArtifact<*>) -> Unit,
        onHandlersResult: (Step) -> Unit
    ): Boolean {
        var inputArtifact = produceStartingArtifact()

        for (step in testConfiguration.steps) {
            if (!shouldRunStep(step, inputArtifact)) continue

            val thereWereCriticalExceptionsOnPreviousSteps = allFailedExceptions.any { it.failureDisablesNextSteps }
            when (val result = runStep.run(step, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps)) {
                is TestStep.StepResult.Artifact<*> -> {
                    require(step is TestStep.FacadeStep<*, *>)
                    onArtifactResult(result.outputArtifact)
                    inputArtifact = result.outputArtifact
                }
                is TestStep.StepResult.ErrorFromFacade -> {
                    allFailedExceptions += result.exception
                    return false
                }
                is TestStep.StepResult.HandlersResult -> {
                    val (exceptionsFromHandlers, shouldRunNextSteps) = result
                    allFailedExceptions += exceptionsFromHandlers
                    onHandlersResult(step)
                    if (!shouldRunNextSteps) {
                        return false
                    }
                }
                is TestStep.StepResult.NoArtifactFromFacade -> return false
            }
        }
        return true
    }

    /**
     * 提供 `filterFailedExceptions` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected fun filterFailedExceptions(failedExceptions: List<WrappedException>): List<Throwable> {
        return testConfiguration.afterAnalysisCheckers
            .fold(failedExceptions) { assertions, checker ->
                checker.suppressIfNeeded(assertions)
            }
            .sorted()
            .map { it.cause }
    }

    /**
     * 提供 `withAssertionCatching` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected inline fun withAssertionCatching(exceptionWrapper: (Throwable) -> WrappedException, block: () -> Unit): Boolean {
        return try {
            block()
            false
        } catch (e: Throwable) {
            allFailedExceptions += exceptionWrapper(e)
            true
        }
    }
}
