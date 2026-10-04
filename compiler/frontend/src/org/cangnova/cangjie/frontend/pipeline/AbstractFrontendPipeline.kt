package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartChirPaths
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartCjoPaths
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.cjoOutputFile
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.phaser.CompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserProfiler
import org.cangnova.cangjie.phaser.invokeToplevel
import java.nio.file.Files
import java.nio.file.Path

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
     * 把 CJMP 编译模式输入落到配置上，供 session 工厂推导编译模式：
     * - common-part 输入（`-Xcjmp-common-part` / `-Xcjmp-common-part-chir`，官方
     *   `--common-part-cjo` / `--common-part-chir` 对位）非空即 specific 编译；
     * - `-Xcjmp-compile-common`（官方 `--output-type=chir` 对位）即 common 编译。
     *
     * 两个列表必须一一配对（官方 `driver_require_common_chir_for_each_common_cjo`）；
     * 违反时按 driver 错误报告并终止，而不是留到 session 构造期以异常形式暴露。
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
        configuration.cjmpChirOutput = arguments.cjmpCompileCommon
        return true
    }

    /**
     * 创建并执行阶段化前端管线。
     *
     * 阶段耗时采集由命令行开关驱动：`--report-perf` 报告到 stderr，`--dump-perf <path>` 落文件。
     * 两者都没给时不创建采集器，`PhaseConfig.profiler` 为 null，phase 路径与开启 profiling 前
     * 完全一致（不取单调时钟、不分配记录对象）。报告在管线退出时统一输出——即使管线因编译错误
     * 中断，已经跑过的 phase 耗时也要留下，这正是排查失败位置需要的信息。
     */
    private fun runPhasedPipeline(input: ArgumentsPipelineArtifact<A>): Boolean {
        val compoundPhase = createCompoundPhase(input.arguments)
        val profiler = createPhaseProfiler(input.arguments)
        val phaseConfig = PhaseConfig(profiler = profiler)
        val context = PipelineContext(input.configuration)

        return try {
            compoundPhase.invokeToplevel(phaseConfig, context, input)
            true
        } catch (e: PipelineStepException) {
            !e.definitelyCompilationError
        } finally {
            profiler?.let { emitPhaseProfile(it, input) }
        }
    }

    /**
     * 按命令行开关创建阶段耗时采集器；两个开关都没开时返回 null。
     */
    private fun createPhaseProfiler(arguments: A): PhaserProfiler? =
        if (arguments.reportPerf || arguments.dumpPerf != null) PhaserProfiler() else null

    /**
     * 输出阶段耗时报告。
     *
     * 落文件失败只报诊断、不改变编译结果：性能报告是辅助产物，写不出去不应该让编译失败。
     */
    private fun emitPhaseProfile(profiler: PhaserProfiler, input: ArgumentsPipelineArtifact<A>) {
        val report = profiler.renderReport()
        writePhaseProfileReport(
            report = report,
            dumpPath = input.arguments.dumpPerf,
            reportToConsole = input.arguments.reportPerf,
            messageCollector = input.configuration.messageCollector,
        )
    }

    /**
     * 根据参数创建实际执行的复合编译阶段。
     */
    abstract fun createCompoundPhase(arguments: A): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<A>, *>
}

/**
 * 把阶段耗时报告写到 [dumpPath] 指定的位置，并在 [reportToConsole] 为真时输出到 stderr。
 *
 * 写 stderr 而不是 stdout：编译器的 stdout 可能被工具解析（例如生成物路径），性能报告混进去
 * 会污染这类解析。与 [org.cangnova.cangjie.phaser.LoggingContext.log] 的通道一致。
 *
 * 落文件失败只报 ERROR 诊断、不抛出：性能报告是辅助产物，写不出去不应该让编译失败。
 */
internal fun writePhaseProfileReport(
    report: String,
    dumpPath: String?,
    reportToConsole: Boolean,
    messageCollector: MessageCollector,
) {
    if (dumpPath != null) {
        runCatching { Files.writeString(Path.of(dumpPath), report) }
            .onFailure {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Failed to write phase profile to \"$dumpPath\": ${it.message}",
                )
            }
    }

    if (reportToConsole) {
        System.err.print(report)
    }
}
