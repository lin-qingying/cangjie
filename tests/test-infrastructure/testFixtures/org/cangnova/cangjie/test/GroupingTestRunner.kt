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

    /**
     * 执行 `run` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `EmptyModuleStructure` 单例，集中承载测试基础设施的共享状态、常量或默认行为。
     */
    private object EmptyModuleStructure : TestModuleStructure() {
        /**
         * 保存 `modules`，供测试基础设施在测试执行期间读取或传递。
         */
        override val modules get() = emptyList<org.cangnova.cangjie.test.model.TestModule>()
        /**
         * 保存 `allDirectives`，供测试基础设施在测试执行期间读取或传递。
         */
        override val allDirectives get() = org.cangnova.cangjie.test.directives.model.RegisteredDirectivesImpl(emptyList(), emptyMap(), emptyMap())
        /**
         * 保存 `originalTestDataFiles`，供测试基础设施在测试执行期间读取或传递。
         */
        override val originalTestDataFiles get() = emptyList<File>()
    }

    /**
     * 提供 `GroupingPhaseStep` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun TestStep.GroupingPhaseStep<*, *>.hackyProcess(
        inputArtifact: ResultingArtifact<*>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return (this as TestStep.GroupingPhaseStep<GroupingPhaseInputArtifact, *>)
            .process(inputArtifact as ResultingArtifact<GroupingPhaseInputArtifact>, thereWereExceptionsOnPreviousSteps)
    }

    /**
     * 提供 `>` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun <I : ResultingArtifact<I>> TestStep.GroupingPhaseStep<I, *>.process(
        artifact: ResultingArtifact<I>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return this.process(artifact as I, thereWereExceptionsOnPreviousSteps)
    }
}
