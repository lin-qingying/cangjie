package org.cangnova.cangjie.cli.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.phaser.*

/**
 * 管线产物基类（对齐 K2 的 PipelineArtifact）
 */
abstract class PipelineArtifact {
    abstract val configuration: CompilerConfiguration

    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    annotation class CliPipelineInternals(val message: String)

    @CliPipelineInternals(OPT_IN_MESSAGE)
    abstract fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact

    companion object {
        const val OPT_IN_MESSAGE = "This method is intended to be used only by utility `withNewDiagnosticCollector`"
    }
}

/**
 * 带退出码的管线产物
 */
abstract class PipelineArtifactWithExitCode : PipelineArtifact() {
    abstract val exitCode: Int
}

/**
 * 管线阶段抽象（对齐 K2 的 PipelinePhase）
 */
abstract class PipelinePhase<I : PipelineArtifact, O : PipelineArtifact>(
    name: String,
    preActions: Set<Action<I, PipelineContext>> = emptySet(),
    postActions: Set<Action<O, PipelineContext>> = emptySet(),
) : NamedCompilerPhase<PipelineContext, I, O>(
    name = name,
    preactions = preActions,
    postactions = postActions.mapTo(mutableSetOf()) { it.toPostAction() }
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
