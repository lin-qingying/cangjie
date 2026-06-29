package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.phaser.CompilerPhase

/**
 * 将两个前端管线阶段顺序组合为一个复合阶段。
 */
private infix fun <I : PipelineArtifact, M : PipelineArtifact, O : PipelineArtifact> PipelinePhase<I, M>.then(
    next: PipelinePhase<M, O>
): CompilerPhase<PipelineContext, I, O> {
    return CompoundPipelinePhase(this, next)
}

/**
 * 顺序执行两个管线阶段的复合阶段。
 */
private class CompoundPipelinePhase<I : PipelineArtifact, M : PipelineArtifact, O : PipelineArtifact>(
    /**
     * 先执行的阶段。
     */
    private val first: PipelinePhase<I, M>,
    /**
     * 接收 [first] 输出并继续执行的阶段。
     */
    private val second: PipelinePhase<M, O>,
) : PipelinePhase<I, O>("${first.name} then ${second.name}") {

    /**
     * 执行组合阶段；任一阶段返回 `null` 时终止整个组合。
     */
    override fun executePhase(input: I): O? {
        val intermediate = first.executePhase(input) ?: return null
        return second.executePhase(intermediate)
    }
}
