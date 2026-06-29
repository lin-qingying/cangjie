package org.cangnova.cangjie.test

import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.create
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.TestArtifactKind
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

/**
 * 表示 `NonGroupingPhaseOutput`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
data class NonGroupingPhaseOutput(
    /**
     * 保存 `artifact`，供测试基础设施在测试执行期间读取或传递。
     */
    val artifact: ResultingArtifact<*>,
    /**
     * 保存 `testServices`，供测试基础设施在测试执行期间读取或传递。
     */
    val testServices: TestServices,
)

/**
 * 提供 `GroupingPhaseInputKind` 单例，集中承载测试基础设施的共享状态、常量或默认行为。
 */
object GroupingPhaseInputKind : TestArtifactKind<GroupingPhaseInputArtifact>("GroupingPhaseInput")

/**
 * 表示 `GroupingPhaseInputArtifact`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
class GroupingPhaseInputArtifact(
    /**
     * 保存 `configuration`，供测试基础设施在测试执行期间读取或传递。
     */
    val configuration: CompilerConfiguration,
    /**
     * 保存 `outputs`，供测试基础设施在测试执行期间读取或传递。
     */
    val outputs: List<NonGroupingPhaseOutput>,
) : ResultingArtifact<GroupingPhaseInputArtifact>() {
    /**
     * 保存 `kind`，供测试基础设施在测试执行期间读取或传递。
     */
    override val kind: TestArtifactKind<GroupingPhaseInputArtifact>
        get() = GroupingPhaseInputKind
}

/**
 * 表示 `GroupingPhaseInputsHolder`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
class GroupingPhaseInputsHolder(
    /**
     * 保存 `outputs`，供测试基础设施在测试执行期间读取或传递。
     */
    val outputs: List<NonGroupingPhaseOutput>,
) : TestService

/**
 * 表示 `GroupingPhaseInputsMerger`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
class GroupingPhaseInputsMerger(
    /**
     * 保存 `testServices`，供测试基础设施在测试执行期间读取或传递。
     */
    private val testServices: TestServices,
    /**
     * 保存 `workers`，供测试基础设施在测试执行期间读取或传递。
     */
    private val workers: List<Worker>,
) {
    /**
     * 执行 `merge` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun merge(nonGroupingPhaseOutputs: List<NonGroupingPhaseOutput>): GroupingPhaseInputArtifact {
        val secondPhaseConfiguration = CompilerConfiguration.create(messageCollector = MessageCollector.NONE)
        val firstPhaseServices = nonGroupingPhaseOutputs.map { it.testServices }
        workers.forEach { worker ->
            worker.process(secondPhaseConfiguration, firstPhaseServices)
        }
        return GroupingPhaseInputArtifact(secondPhaseConfiguration, nonGroupingPhaseOutputs)
    }

    /**
     * 表示 `Worker`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    abstract class Worker(protected val testServices: TestServices) {
        /**
         * 提供 `process` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        abstract fun process(configuration: CompilerConfiguration, firstPhaseServices: List<TestServices>)
    }
}
