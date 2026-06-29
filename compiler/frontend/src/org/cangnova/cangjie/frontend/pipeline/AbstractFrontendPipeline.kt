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

    /**
     * 从命令行参数和编译配置启动前端管线。
     */
    fun execute(arguments: A, configuration: CompilerConfiguration): Boolean {
        val input = ArgumentsPipelineArtifact(arguments, configuration)
        return runPhasedPipeline(input)
    }

    /**
     * 创建并执行阶段化前端管线。
     */
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

    /**
     * 根据参数创建实际执行的复合编译阶段。
     */
    abstract fun createCompoundPhase(arguments: A): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<A>, *>
}
