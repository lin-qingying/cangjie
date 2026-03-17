package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import java.io.File

/**
 * 分组测试运行器
 *
 * 对应 Kotlin K2 的 GroupingTestRunner
 */
class GroupingTestRunner(
    testConfiguration: GroupingPhaseTestConfiguration
) : TestRunner<TestStep.GroupingPhaseStep<*, *>, GroupingPhaseTestConfiguration>(testConfiguration) {
    init {
        testServices.register(TestModuleStructure::class, EmptyModuleStructure)
    }

    fun run(nonGroupingPhaseOutputs: List<NonGroupingPhaseOutput>) {
        testServices.register(GroupingPhaseInputsHolder::class, GroupingPhaseInputsHolder(nonGroupingPhaseOutputs))
        val merger = GroupingPhaseInputsMerger(testServices, testConfiguration.mergerWorkers)
        runPipelineOnSingleUnit(
            produceStartingArtifact = { merger.merge(nonGroupingPhaseOutputs) },
            shouldRunStep = { _, _ -> true },
            runStep = { step, input, thereWereCriticalExceptionsOnPreviousSteps ->
                step.hackyProcess(input, thereWereCriticalExceptionsOnPreviousSteps)
            },
            onArtifactResult = {},
            onHandlersResult = {}
        )
    }

    private object EmptyModuleStructure : TestModuleStructure() {
        override val modules get() = emptyList<org.cangnova.cangjie.test.model.TestModule>()
        override val allDirectives get() = org.cangnova.cangjie.test.directives.model.RegisteredDirectivesImpl(emptyList(), emptyMap(), emptyMap())
        override val originalTestDataFiles get() = emptyList<File>()
    }

    private fun TestStep.GroupingPhaseStep<*, *>.hackyProcess(
        inputArtifact: ResultingArtifact<*>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return (this as TestStep.GroupingPhaseStep<GroupingPhaseInputArtifact, *>)
            .process(inputArtifact as ResultingArtifact<GroupingPhaseInputArtifact>, thereWereExceptionsOnPreviousSteps)
    }

    private fun <I : ResultingArtifact<I>> TestStep.GroupingPhaseStep<I, *>.process(
        artifact: ResultingArtifact<I>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return this.process(artifact as I, thereWereExceptionsOnPreviousSteps)
    }
}
