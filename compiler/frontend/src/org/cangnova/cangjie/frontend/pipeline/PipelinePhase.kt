package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.phaser.Action
import org.cangnova.cangjie.phaser.ActionState
import org.cangnova.cangjie.phaser.NamedCompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserState

/**
 * 管线产物基类（对齐 K2 的 PipelineArtifact）。
 */
abstract class PipelineArtifact {
    /**
     * 当前产物携带的编译器配置。
     */
    abstract val configuration: CompilerConfiguration

    /**
     * 标记仅允许前端管线内部使用的 API。
     */
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    annotation class FrontendPipelineInternals(val message: String)

    /**
     * 使用新的编译器配置复制当前管线产物。
     */
    @FrontendPipelineInternals(OPT_IN_MESSAGE)
    abstract fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact

    /**
     * [withCompilerConfiguration] 的 opt-in 提示文本。
     */
    companion object {
        const val OPT_IN_MESSAGE = "This method is intended to be used only by utility `withNewDiagnosticCollector`"
    }
}

/**
 * 管线阶段抽象（对齐 K2 的 PipelinePhase）。
 */
abstract class PipelinePhase<I : PipelineArtifact, O : PipelineArtifact>(
    name: String,
    preActions: Set<Action<I, PipelineContext>> = emptySet(),
    postActions: Set<Action<O, PipelineContext>> = emptySet(),
) : NamedCompilerPhase<PipelineContext, I, O>(
    name = name,
    preactions = preActions,
    postactions = postActions.mapTo(mutableSetOf()) { it.toPostAction() },
) {
    /**
     * 执行阶段主体，并将 `null` 输出转换为管线中断异常。
     */
    final override fun phaseBody(context: PipelineContext, input: I): O {
        return executePhase(input) ?: throw PipelineStepException()
    }

    /**
     * 执行具体阶段逻辑。
     *
     * 返回 `null` 表示阶段无法继续产出有效结果。
     */
    abstract fun executePhase(input: I): O?

    /**
     * 前端管线阶段不支持禁用后继续返回默认输出。
     */
    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfig,
        phaserState: PhaserState,
        context: PipelineContext,
        input: I,
    ): O {
        error("Phase $name should not be called when disabled")
    }
}

/**
 * 表示前端管线阶段执行失败或主动终止。
 */
class PipelineStepException(
    /**
     * 该终止是否一定由编译错误导致。
     */
    val definitelyCompilationError: Boolean = false,
) : RuntimeException()

/**
 * 用异常信号表示管线已经成功完成并可提前退出。
 */
class SuccessfulPipelineExecutionException : RuntimeException()

/**
 * 将只接收输出产物的动作适配为接收输入输出对的后置动作。
 */
private fun <Input, Output, Context> Action<Output, Context>.toPostAction(): Action<Pair<Input, Output>, Context> {
    return { state: ActionState, inputOutput: Pair<Input, Output>, context: Context ->
        this.invoke(state, inputOutput.second, context)
    }
}
