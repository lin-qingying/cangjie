package org.cangnova.cangjie.cli.pipeline

import org.cangnova.cangjie.cli.common.arguments.CommonCompilerArguments
import org.cangnova.cangjie.phaser.CompilerPhase


private infix fun <I : PipelineArtifact, M : PipelineArtifact, O : PipelineArtifact> PipelinePhase<I, M>.then(
    next: PipelinePhase<M, O>
): CompilerPhase<PipelineContext, I, O> {
    return CompoundPipelinePhase(this, next)
}

private class CompoundPipelinePhase<I : PipelineArtifact, M : PipelineArtifact, O : PipelineArtifact>(
    private val first: PipelinePhase<I, M>,
    private val second: PipelinePhase<M, O>,
) : PipelinePhase<I, O>("${first.name} then ${second.name}") {

    override fun executePhase(input: I): O? {
        val intermediate = first.executePhase(input) ?: return null
        return second.executePhase(intermediate)
    }
}
