package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartChirPaths
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartCjoPaths
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.cjoOutputFile
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.messages.CompilerMessageSeverity
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
        // 一个 CompilerConfiguration 可以被多个 frontend invocation 复用；
        // 输出路径是本次执行的事实，不能继承上一次 .cj.d 编译的状态。
        configuration.cjoOutputDirectory = null
        configuration.cjoOutputFile = null
        if (arguments.compileCjd) {
            configuration.cjoOutputDirectory = arguments.outputDirectory
            configuration.cjoOutputFile = arguments.outputFile
        }
        if (!configuration.configureLanguageVersionSettings(arguments.languageVersion)) return false
        if (!configureCjmpCommonPartInputs(arguments, configuration)) return false
        val input = ArgumentsPipelineArtifact(arguments, configuration)
        return runPhasedPipeline(input)
    }

    /**
     * 把 CJMP common-part 输入（`-Xcjmp-common-part` / `-Xcjmp-common-part-chir`，官方
     * `--common-part-cjo` / `--common-part-chir` 对位）落到配置上，供 session 工厂推导编译模式。
     *
     * 两个列表必须一一配对（官方 `driver_require_common_chir_for_each_common_cjo`）；
     * 违反时按 driver 错误报告并终止，而不是留到 session 构造期以异常形式暴露。
     * CHIR 输出模式（官方 Common 判据）在本仓库 driver 尚无对位选项，Common 模式目前
     * 只经测试指令 / IDE 模块 kind 显式注入。
     */
    private fun configureCjmpCommonPartInputs(arguments: A, configuration: CompilerConfiguration): Boolean {
        val cjoPaths = arguments.cjmpCommonPart.toList()
        val chirPaths = arguments.cjmpCommonPartChir.toList()
        if (cjoPaths.size != chirPaths.size) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "common .chir files count should be equal to common .cjo files count",
            )
            return false
        }
        configuration.cjmpCommonPartCjoPaths = cjoPaths
        configuration.cjmpCommonPartChirPaths = chirPaths
        return true
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
