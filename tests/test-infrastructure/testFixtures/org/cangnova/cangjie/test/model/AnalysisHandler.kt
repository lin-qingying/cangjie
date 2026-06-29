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
    /**
     * 保存 `testServices`，供测试模型在测试执行期间读取或传递。
     */
    val testServices: TestServices,
    /**
     * 保存 `failureDisablesNextSteps`，供测试模型在测试执行期间读取或传递。
     */
    val failureDisablesNextSteps: Boolean,
    /**
     * 保存 `doNotRunIfThereWerePreviousFailures`，供测试模型在测试执行期间读取或传递。
     */
    val doNotRunIfThereWerePreviousFailures: Boolean
) : ServicesAndDirectivesContainer {
    /**
     * 保存 `additionalAfterAnalysisCheckers`，供测试模型在测试执行期间读取或传递。
     */
    open val additionalAfterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
        get() = emptyList()

    /**
     * 保存 `assertions`，供测试模型在测试执行期间读取或传递。
     */
    protected val assertions: Assertions
        get() = testServices.assertions
    /**
     * 提供 `setCompilationStage` 对应的测试模型流程，维持测试框架的阶段契约。
     */
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
    /**
     * 保存 `artifactKind`，供测试模型在测试执行期间读取或传递。
     */
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
    /**
     * 提供 `processModule` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    abstract fun processModule(module: TestModule, info: A)
    /**
     * 提供 `processAfterAllModules` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    abstract fun processAfterAllModules(someAssertionWasFailed: Boolean)
}

/**
 * 前端输出处理器
 *
 * 对应 Kotlin K2 的 FrontendOutputHandler
 */
abstract class FrontendOutputHandler<R : ResultingArtifact.FrontendOutput<R>>(
    testServices: TestServices,
    /**
     * 保存 `artifactKind`，供测试模型在测试执行期间读取或传递。
     */
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
    /**
     * 保存 `artifactKind`，供测试模型在测试执行期间读取或传递。
     */
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
    /**
     * 保存 `artifactKind`，供测试模型在测试执行期间读取或传递。
     */
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
    /**
     * 提供 `processArtifact` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    abstract fun processArtifact(artifact: A)
}
