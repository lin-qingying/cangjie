package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.phaser.NamedCompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserState

/**
 * 将两个前端管线阶段顺序组合为一个复合阶段。
 */
internal infix fun <I : PipelineArtifact, M : PipelineArtifact, O : PipelineArtifact> PipelinePhase<I, M>.then(
    next: PipelinePhase<M, O>
): PipelinePhase<I, O> {
    return CompoundPipelinePhase(this, next)
}

/**
 * CFIR 前端管线的完整阶段序列：前端分析，随后写出 CJO。
 *
 * 测试基建与生产入口共用同一个序列：阶段顺序只有这一处定义，新增阶段不会在两边悄悄分叉。
 */
fun cfirFrontendPipeline(): PipelinePhase<ConfigurationPipelineArtifact, DefaultCfirFrontendPipelineArtifact> {
    return CfirFrontendPipelinePhase then CjoWritePipelinePhase
}

/**
 * 顺序执行两个管线阶段的复合阶段。
 *
 * 子阶段经由 [NamedCompilerPhase.invoke] 执行，与它们独立执行时走完全相同的协议：启用判断、
 * prerequisite 校验、前后置 action、阶段耗时采集一视同仁。直接调子阶段的 [executePhase]
 * 会跳过这些步骤，组合出来的管线就成了一串不受配置与采集管理的裸调用。
 *
 * 复合阶段自身不记录耗时：它的耗时约等于两个子阶段之和，若在同一深度再记一份，报告合计
 * 会把同一段时间算两遍。子阶段因此与复合阶段同深度，报告里是两条平铺的真实阶段，而不是
 * 一层套一层的自我包含。
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
     * 按管线协议依次执行两个子阶段。
     */
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: PipelineContext, input: I): O {
        val intermediate = first.invoke(phaseConfig, phaserState, context, input)
        return second.invoke(phaseConfig, phaserState, context, intermediate)
    }

    /**
     * 绕过管线协议直接执行两个阶段；只用于不需要配置与采集的场合（测试夹具直接驱动单个
     * 阶段逻辑）。此时不采集阶段耗时，也不校验 prerequisite。
     */
    override fun executePhase(input: I): O? {
        val intermediate = first.executePhase(input) ?: return null
        return second.executePhase(intermediate)
    }

    /**
     * 对外展示的是两个真实阶段，而不是这个组合外壳。
     */
    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<*, *, *>>> =
        first.getNamedSubphases(startDepth) + second.getNamedSubphases(startDepth)
}