package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.arguments.configureWarningOptions
import org.cangnova.cangjie.frontend.pipeline.CheckCompilationErrors.CheckDiagnosticCollector
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.entrypoint.configuration.CjmpCommonPartInputConsumer
import org.cangnova.cangjie.cfir.entrypoint.configuration.CjmpCommonPartInputResolution
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleDebug
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleOptLevel
import org.cangnova.cangjie.cfir.entrypoint.configuration.configureCjmpCommonPartInputs
import org.cangnova.cangjie.cfir.session.CfirCjmpOptimizationLevel
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.cjoOutputFile
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.phaser.CompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.invokeToplevel
import java.nio.file.Path

/**
 * 前端管线编排器。
 */
abstract class AbstractFrontendPipeline<A : CommonCompilerArguments> {

    /**
     * 从命令行参数和编译配置启动前端管线。
     */
    fun execute(arguments: A, configuration: CompilerConfiguration): Boolean =
        executeWithArtifact(arguments, configuration).succeeded

    /**
     * 执行前端管线并返回最终阶段产物，供后续 CHIR 转换、链接与后端阶段消费。
     */
    fun executeWithArtifact(
        arguments: A,
        configuration: CompilerConfiguration,
    ): FrontendPipelineExecutionResult {
        if (!configuration.configureWarningOptions(arguments)) {
            return FrontendPipelineExecutionResult(succeeded = false, artifact = null)
        }
        // 一个 CompilerConfiguration 可以被多个 frontend invocation 复用；
        // 输出路径是本次执行的事实，不能继承上一次 .cj.d 编译的状态。
        configuration.cjoOutputDirectory = null
        configuration.cjoOutputFile = null
        if (arguments.outputDirectory != null && arguments.outputFile?.let { Path.of(it).isAbsolute } == true) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                CangJieParsingBundle.message("cli.error.output.file.absolute.with.output.dir"),
            )
            return FrontendPipelineExecutionResult(succeeded = false, artifact = null)
        }
        configuration.cjoOutputDirectory = arguments.outputDirectory
        configuration.cjoOutputFile = arguments.outputFile
        if (!arguments.compileCjd && arguments.cjmpCompileCommon &&
            arguments.outputDirectory == null && arguments.outputFile == null
        ) {
            // 官方 cjc 未指定输出参数时以当前目录作为输出目录；common 模式必须落盘其 CJO。
            configuration.cjoOutputDirectory = "."
        }
        if (!configuration.configureLanguageVersionSettings(arguments.languageVersion)) {
            return FrontendPipelineExecutionResult(succeeded = false, artifact = null)
        }
        val requestedOptimizationLevel = arguments.optimizationLevel
        val optimizationLevel = requestedOptimizationLevel?.let(CfirCjmpOptimizationLevel::fromCommandLineValue)
        if (requestedOptimizationLevel != null && optimizationLevel == null) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                CangJieParsingBundle.message(
                    "cli.error.invalid.cjmp.optimization.level",
                    requestedOptimizationLevel,
                ),
            )
            return FrontendPipelineExecutionResult(succeeded = false, artifact = null)
        }
        configuration.cjmpModuleDebug = arguments.debug
        configuration.cjmpModuleOptLevel = optimizationLevel ?: CfirCjmpOptimizationLevel.O0
        if (!configureCjmpCommonPartInputs(arguments, configuration)) {
            return FrontendPipelineExecutionResult(succeeded = false, artifact = null)
        }
        val input = ArgumentsPipelineArtifact(arguments, configuration)
        val result = runPhasedPipeline(input)
        return if (CheckDiagnosticCollector.checkHasErrors(configuration)) result.copy(succeeded = false) else result
    }

    /**
     * 按语言版本解析 common-part 参数并写入唯一配置结果。
     * 旧协议保留最后一个 CJO；alpha.20 起按顺序配对 CJO/CHIR，配对错误在进入前端阶段前报告。
     * common 编译模式由独立的 CHIR 输出选项决定。
     */
    private fun configureCjmpCommonPartInputs(arguments: A, configuration: CompilerConfiguration): Boolean {
        val cjoPaths = arguments.cjmpCommonPart.toList()
        val chirPaths = arguments.cjmpCommonPartChir.toList()
        val resolution = configuration.configureCjmpCommonPartInputs(
            commonPartCjoPaths = cjoPaths,
            commonPartChirPaths = chirPaths,
            consumer = CjmpCommonPartInputConsumer.COMPILATION_DRIVER,
        )
        if (resolution is CjmpCommonPartInputResolution.Invalid) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                resolution.message,
            )
            return false
        }
        configuration.cjmpChirOutput = arguments.cjmpCompileCommon
        return true
    }

    /**
     * 创建并执行阶段化前端管线。
     */
    private fun runPhasedPipeline(input: ArgumentsPipelineArtifact<A>): FrontendPipelineExecutionResult {
        val compoundPhase = createCompoundPhase(input.arguments)
        val phaseConfig = PhaseConfig()
        val context = PipelineContext(input.configuration)

        return try {
            val output = compoundPhase.invokeToplevel(phaseConfig, context, input)
            val artifact = output as? PipelineArtifact
                ?: error("Frontend pipeline must finish with a PipelineArtifact")
            FrontendPipelineExecutionResult(
                succeeded = true,
                artifact = artifact,
            )
        } catch (e: PipelineStepException) {
            // 阶段中断可以是正常提前退出；结合明确错误标记和两个收集器的实际错误判定成败。
            val configuration = input.configuration
            if (e.definitelyCompilationError || CheckDiagnosticCollector.checkHasErrors(configuration)) {
                FrontendPipelineExecutionResult(
                    succeeded = false,
                    artifact = null,
                )
            } else {
                FrontendPipelineExecutionResult(
                    succeeded = true,
                    artifact = null,
                )
            }
        } catch (_: SuccessfulPipelineExecutionException) {
            FrontendPipelineExecutionResult(
                succeeded = true,
                artifact = null,
            )
        }
    }

    /**
     * 根据参数创建实际执行的复合编译阶段。
     */
    abstract fun createCompoundPhase(arguments: A): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<A>, *>
}
