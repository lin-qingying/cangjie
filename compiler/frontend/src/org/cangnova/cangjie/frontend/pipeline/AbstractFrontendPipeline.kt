package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.phaser.CompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.invokeToplevel

/**
 * 前端管线编排器。
 */
abstract class AbstractFrontendPipeline<A : CommonCompilerArguments> {

    fun execute(arguments: A, configuration: CompilerConfiguration): Boolean {
        val input = ArgumentsPipelineArtifact(arguments, configuration)
        return runPhasedPipeline(input)
    }

    private fun runPhasedPipeline(input: ArgumentsPipelineArtifact<A>): Boolean {
        val compoundPhase = createCompoundPhase(input.arguments)
        val phaseConfig = PhaseConfig()
        val context = PipelineContext(input.configuration)

        return try {
            compoundPhase.invokeToplevel(phaseConfig, context, input)
            true
        } catch (e: PipelineStepException) {
            !e.definitelyCompilationError
        }
    }

    abstract fun createCompoundPhase(arguments: A): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<A>, *>
}
