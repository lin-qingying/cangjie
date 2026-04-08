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
    abstract val configuration: CompilerConfiguration

    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    annotation class FrontendPipelineInternals(val message: String)

    @FrontendPipelineInternals(OPT_IN_MESSAGE)
    abstract fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact

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
    final override fun phaseBody(context: PipelineContext, input: I): O {
        return executePhase(input) ?: throw PipelineStepException()
    }

    abstract fun executePhase(input: I): O?

    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfig,
        phaserState: PhaserState,
        context: PipelineContext,
        input: I,
    ): O {
        error("Phase $name should not be called when disabled")
    }
}

class PipelineStepException(val definitelyCompilationError: Boolean = false) : RuntimeException()
class SuccessfulPipelineExecutionException : RuntimeException()

private fun <Input, Output, Context> Action<Output, Context>.toPostAction(): Action<Pair<Input, Output>, Context> {
    return { state: ActionState, inputOutput: Pair<Input, Output>, context: Context ->
        this.invoke(state, inputOutput.second, context)
    }
}
