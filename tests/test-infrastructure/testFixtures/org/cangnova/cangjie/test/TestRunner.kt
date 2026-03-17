package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.services.*
import java.io.IOException

/**
 * 测试运行器
 *
 * 对应 Kotlin K2 的 TestRunner
 */
sealed class

TestRunner<Step : TestStep<*, *>, Configuration : TestConfiguration<Step>>(val testConfiguration: Configuration) {
    val testServices: TestServices get() = testConfiguration.testServices
    protected val allFailedExceptions = mutableListOf<WrappedException>()

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

    fun reportFailures() {
        val filteredFailedAssertions = filterFailedExceptions(allFailedExceptions)
        testServices.assertions.failAll(filteredFailedAssertions)
    }

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

    protected fun interface RunStep<Step : TestStep<*, *>> {
        fun run(
            step: Step,
            inputArtifact: ResultingArtifact<*>,
            thereWereCriticalExceptionsOnPreviousSteps: Boolean
        ): TestStep.StepResult<*>
    }

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

    protected fun filterFailedExceptions(failedExceptions: List<WrappedException>): List<Throwable> {
        return testConfiguration.afterAnalysisCheckers
            .fold(failedExceptions) { assertions, checker ->
                checker.suppressIfNeeded(assertions)
            }
            .sorted()
            .map { it.cause }
    }

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
