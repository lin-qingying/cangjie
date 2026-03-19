package org.cangnova.cangjie.test

import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.create
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.TestArtifactKind
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

data class NonGroupingPhaseOutput(
    val artifact: ResultingArtifact<*>,
    val testServices: TestServices,
)

object GroupingPhaseInputKind : TestArtifactKind<GroupingPhaseInputArtifact>("GroupingPhaseInput")

class GroupingPhaseInputArtifact(
    val configuration: CompilerConfiguration,
    val outputs: List<NonGroupingPhaseOutput>,
) : ResultingArtifact<GroupingPhaseInputArtifact>() {
    override val kind: TestArtifactKind<GroupingPhaseInputArtifact>
        get() = GroupingPhaseInputKind
}

class GroupingPhaseInputsHolder(
    val outputs: List<NonGroupingPhaseOutput>,
) : TestService

class GroupingPhaseInputsMerger(
    private val testServices: TestServices,
    private val workers: List<Worker>,
) {
    fun merge(nonGroupingPhaseOutputs: List<NonGroupingPhaseOutput>): GroupingPhaseInputArtifact {
        val secondPhaseConfiguration = CompilerConfiguration.create(messageCollector = MessageCollector.NONE)
        val firstPhaseServices = nonGroupingPhaseOutputs.map { it.testServices }
        workers.forEach { worker ->
            worker.process(secondPhaseConfiguration, firstPhaseServices)
        }
        return GroupingPhaseInputArtifact(secondPhaseConfiguration, nonGroupingPhaseOutputs)
    }

    abstract class Worker(protected val testServices: TestServices) {
        abstract fun process(configuration: CompilerConfiguration, firstPhaseServices: List<TestServices>)
    }
}
