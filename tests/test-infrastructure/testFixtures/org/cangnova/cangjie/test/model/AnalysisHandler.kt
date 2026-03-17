package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.Assertions
import org.cangnova.cangjie.test.Constructor
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.services.CompilationStage
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 分析处理器基类
 *
 * 对应 Kotlin K2 的 AnalysisHandlerBase
 */
abstract class AnalysisHandlerBase<A : ResultingArtifact<A>>(
    val testServices: TestServices,
    val failureDisablesNextSteps: Boolean,
    val doNotRunIfThereWerePreviousFailures: Boolean
) : ServicesAndDirectivesContainer {
    open val additionalAfterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
        get() = emptyList()

    protected val assertions: Assertions
        get() = testServices.assertions
    @TestInfrastructureInternals
    internal fun setCompilationStage(stage: CompilationStage) {
        if (this::compilationStage.isInitialized) {
            error("Compilation stage already initialized for $this")
        }
        compilationStage = stage
    }
    /**
     * The compilation stage this handler is being executed in.
     */
    lateinit var compilationStage: CompilationStage
        private set
    abstract val artifactKind: TestArtifactKind<A>
}

// ----------------------------- non-grouping handlers -----------------------------

/**
 * 非分组分析处理器
 *
 * 对应 Kotlin K2 的 AnalysisHandler
 */
abstract class AnalysisHandler<A : ResultingArtifact<A>>(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean,
    doNotRunIfThereWerePreviousFailures: Boolean
) : AnalysisHandlerBase<A>(testServices, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures) {
    abstract fun processModule(module: TestModule, info: A)
    abstract fun processAfterAllModules(someAssertionWasFailed: Boolean)
}

/**
 * 前端输出处理器
 *
 * 对应 Kotlin K2 的 FrontendOutputHandler
 */
abstract class FrontendOutputHandler<R : ResultingArtifact.FrontendOutput<R>>(
    testServices: TestServices,
    override val artifactKind: FrontendKind<R>,
    failureDisablesNextSteps: Boolean,
    doNotRunIfThereWerePreviousFailures: Boolean
) : AnalysisHandler<R>(testServices, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures)

/**
 * 后端输入处理器
 *
 * 对应 Kotlin K2 的 BackendInputHandler
 */
abstract class BackendInputHandler<I : ResultingArtifact.BackendInput<I>>(
    testServices: TestServices,
    override val artifactKind: BackendKind<I>,
    failureDisablesNextSteps: Boolean,
    doNotRunIfThereWerePreviousFailures: Boolean
) : AnalysisHandler<I>(testServices, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures)

/**
 * 二进制产物处理器
 *
 * 对应 Kotlin K2 的 BinaryArtifactHandler
 */
abstract class BinaryArtifactHandler<A : ResultingArtifact.Binary<A>>(
    testServices: TestServices,
    override val artifactKind: ArtifactKind<A>,
    failureDisablesNextSteps: Boolean,
    doNotRunIfThereWerePreviousFailures: Boolean
) : AnalysisHandler<A>(testServices, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures)

// ----------------------------- grouping handlers -----------------------------

/**
 * 分组阶段处理器
 *
 * 对应 Kotlin K2 的 GroupingPhaseHandler
 */
abstract class GroupingPhaseHandler<A : ResultingArtifact<A>>(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean,
    doNotRunIfThereWerePreviousFailures: Boolean
) : AnalysisHandlerBase<A>(testServices, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures) {
    abstract fun processArtifact(artifact: A)
}
