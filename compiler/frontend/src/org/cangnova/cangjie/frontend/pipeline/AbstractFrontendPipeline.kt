package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.session.ExplicitCfirConditionalCompilationSettings
import org.cangnova.cangjie.config.MockSupportKind
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartChirPaths
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartCjoPaths
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.cjoOutputFile
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.compileMacro
import org.cangnova.cangjie.config.debugMacro
import org.cangnova.cangjie.config.parallelMacroExpansion
import org.cangnova.cangjie.config.exportForTest
import org.cangnova.cangjie.config.compileTestsOnly
import org.cangnova.cangjie.config.diagnosticFormat
import org.cangnova.cangjie.config.errorCountLimit
import org.cangnova.cangjie.config.warningGroupsEnabled
import org.cangnova.cangjie.config.dumpTokens
import org.cangnova.cangjie.config.dumpParse
import org.cangnova.cangjie.config.dumpAst
import org.cangnova.cangjie.config.dumpMacro
import org.cangnova.cangjie.config.typecheckOnly
import org.cangnova.cangjie.cfir.entrypoint.configuration.conditionalCompilationSettings
import org.cangnova.cangjie.cfir.entrypoint.configuration.noPrelude
import org.cangnova.cangjie.cfir.entrypoint.configuration.noSubPackage
import org.cangnova.cangjie.config.enableCompileTest
import org.cangnova.cangjie.config.enableChirJvmCodegen
import org.cangnova.cangjie.config.jvmClassOutputDirectory
import org.cangnova.cangjie.config.mockSupportKind
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.phaser.CompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserProfiler
import org.cangnova.cangjie.phaser.invokeToplevel
import java.nio.file.Files
import java.nio.file.Path
import java.io.IOException

/**
 * `--diagnostic-format` 支持的官方模式集合。
 */
private val DIAGNOSTIC_FORMATS = setOf("json", "txt")

/**
 * 前端管线编排器。
 */
abstract class AbstractFrontendPipeline<A : CommonCompilerArguments> {

    private sealed interface ConditionalCompilationConfigResult {
        data class Valid(val userDefined: Map<String, String>) : ConditionalCompilationConfigResult
        data class Invalid(val message: String) : ConditionalCompilationConfigResult
    }

    /**
     * 从命令行参数和编译配置启动前端管线。
     */
    fun execute(arguments: A, configuration: CompilerConfiguration): Boolean {
        // 参数应用在隔离副本上完成；验证失败时不把半套参数写回调用方，
        // 复用同一个基准配置时也不会累积本轮 import path。
        val invocationConfiguration = configuration.copy()
        invocationConfiguration.cjoOutputDirectory = null
        invocationConfiguration.cjoOutputFile = null
        if (arguments.compileCjd) {
            invocationConfiguration.cjoOutputDirectory = arguments.outputDirectory
            invocationConfiguration.cjoOutputFile = arguments.outputFile
        }
        if (!invocationConfiguration.configureLanguageVersionSettings(arguments.languageVersion)) return false
        if (!configureInputAndModuleOptions(arguments, invocationConfiguration)) return false
        if (!configureDiagnosticsOptions(arguments, invocationConfiguration)) return false
        if (!configureTestAndMacroOptions(arguments, invocationConfiguration)) return false
        if (!configureCjmpCommonPartInputs(arguments, invocationConfiguration)) return false
        configureDumpOptions(arguments, invocationConfiguration)
        val input = ArgumentsPipelineArtifact(arguments, invocationConfiguration)
        return runPhasedPipeline(input)
    }

    /**
     * 把输入与模块结构选项落到配置上：
     * - `--import-path`（官方 GLOBAL 组）：作为 classpath 根进入 session 工厂的 CJO 搜索路径；
     * - `--module-name`（官方 MODULE_NAME）：直接对位 `CommonConfigurationKeys.MODULE_NAME`；
     * - `--no-prelude` / `--no-sub-pkg`：对位前端 session 的既有配置键。
     */
    private fun configureInputAndModuleOptions(arguments: A, configuration: CompilerConfiguration): Boolean {
        for (path in arguments.importPath) {
            if (path.isBlank()) {
                configuration.messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Empty path passed to argument '--import-path'",
                )
                return false
            }
            configuration.addClasspathRoot(path)
        }
        arguments.moduleName?.let { configuration.moduleName = it }
        configuration.noPrelude = arguments.noPrelude
        configuration.noSubPackage = arguments.noSubPackage
        return true
    }

    /**
     * 把测试编译与宏编译模式落到配置上：
     * - `--test` / `--mock` / `--test-only` / `--export-for-test` 对位
     *   `CommonConfigurationKeys.ENABLE_COMPILE_TEST` / `MOCK_SUPPORT_KIND`；
     * - `--compile-macro` / `--debug-macro` / `--parallel-macro-expansion` 记录宏调用模式事实；
     * - `--cfg` 解析为显式 `@When` 条件环境的 user-defined 条目；`--cfg` 错误是 driver 错误，
     *   在进入管线前中止。
     */
    private fun configureTestAndMacroOptions(arguments: A, configuration: CompilerConfiguration): Boolean {
        val testCompilation = arguments.compileTest || arguments.compileTestsOnly
        val mockSupportKind = when (arguments.mock?.lowercase()) {
            null -> configuration.mockSupportKind
            "on" -> MockSupportKind.ON
            "off" -> MockSupportKind.OFF
            "runtime-error" -> MockSupportKind.RUNTIME_ERROR
            else -> {
                configuration.messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Invalid value for argument '--mock': '${arguments.mock}'. Expected on | off | runtime-error.",
                )
                return false
            }
        }
        val userDefinedConditions = when (val result = parseConditionalCompilationConfig(arguments.conditionalCompilationConfig)) {
            is ConditionalCompilationConfigResult.Valid -> result.userDefined
            is ConditionalCompilationConfigResult.Invalid -> {
                configuration.messageCollector.report(CompilerMessageSeverity.ERROR, result.message)
                return false
            }
        }

        configuration.enableCompileTest = configuration.enableCompileTest || testCompilation
        configuration.mockSupportKind = mockSupportKind
        configuration.compileMacro = arguments.compileMacro
        configuration.debugMacro = arguments.debugMacro
        configuration.parallelMacroExpansion = arguments.parallelMacroExpansion
        configuration.exportForTest = arguments.exportForTest
        configuration.compileTestsOnly = arguments.compileTestsOnly
        // `--emit-jvm` / `--jvm-output-dir` 打开可选的 CFIR2CHIR → JVM codegen 阶段；
        // 给了输出目录而没给 `--emit-jvm` 时隐式开启（输出目录意味着要产物）。
        configuration.enableChirJvmCodegen = arguments.emitJvm || arguments.jvmOutputDirectory != null
        configuration.jvmClassOutputDirectory = arguments.jvmOutputDirectory
        if (userDefinedConditions.isNotEmpty() || testCompilation || arguments.debugMacro) {
            val baseSettings = configuration.conditionalCompilationSettings
            if (baseSettings == null) {
                configuration.messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Cannot configure --cfg, --test, --test-only, or --debug-macro without an explicit target and compiler-version environment.",
                )
                return false
            }
            configuration.conditionalCompilationSettings = ExplicitCfirConditionalCompilationSettings(
                backend = baseSettings.backend,
                arch = baseSettings.arch,
                os = baseSettings.os,
                cjcVersion = baseSettings.cjcVersion,
                debug = baseSettings.debug || arguments.debugMacro,
                test = baseSettings.test || testCompilation,
                userDefined = baseSettings.userDefined + userDefinedConditions,
            )
        }
        return true
    }

    /**
     * 把诊断控制选项落到配置上。
     *
     * `--diagnostic-format` 只接受官方两种模式；warn 开关按“后者生效”的官方语义
     * 计算最终启用的告警组集合；`--error-count-limit` 保存原始文本，由诊断报告端解释。
     */
    private fun configureDiagnosticsOptions(arguments: A, configuration: CompilerConfiguration): Boolean {
        val format = arguments.diagnosticFormat?.lowercase()
        if (format != null && format !in DIAGNOSTIC_FORMATS) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Invalid value for argument '--diagnostic-format': '${arguments.diagnosticFormat}'. " +
                        "Expected ${DIAGNOSTIC_FORMATS.joinToString(" | ")}",
            )
            return false
        }
        configuration.diagnosticFormat = arguments.diagnosticFormat
        configuration.errorCountLimit = arguments.errorCountLimit
        val enabledGroups = arguments.warnOn.toSet() - arguments.warnOff.toSet()
        configuration.warningGroupsEnabled = enabledGroups
        return true
    }

    /**
     * 把 dump/调试族选项落到配置上；这些开关供 phase 内部按需消费。
     */
    private fun configureDumpOptions(arguments: A, configuration: CompilerConfiguration) {
        configuration.dumpTokens = arguments.dumpTokens
        configuration.dumpParse = arguments.dumpParse
        configuration.dumpAst = arguments.dumpAst
        configuration.dumpMacro = arguments.dumpMacro
        configuration.typecheckOnly = arguments.typecheck
    }

    /**
     * 解析 `--cfg` 注入的用户条件。
     *
     * 官方接受两种形态：`key=value` 直接成对，或 `@path` 指向一个每行 `key=value` 的配置文件。
     * 解析失败按 driver 错误报告并中止，而不是静默丢弃条件。
     */
    private fun parseConditionalCompilationConfig(entries: Array<String>): ConditionalCompilationConfigResult {
        val result = linkedMapOf<String, String>()
        for (entry in entries) {
            val trimmed = entry.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("@")) {
                val file = try {
                    Path.of(trimmed.substring(1))
                } catch (failure: java.nio.file.InvalidPathException) {
                    return ConditionalCompilationConfigResult.Invalid(
                        "Invalid config file path '$trimmed': ${failure.message}",
                    )
                }
                if (!Files.isRegularFile(file)) {
                    return ConditionalCompilationConfigResult.Invalid("Config file '$trimmed' does not exist")
                }
                val lines = try {
                    Files.readAllLines(file)
                } catch (failure: IOException) {
                    return ConditionalCompilationConfigResult.Invalid(
                        "Cannot read config file '$trimmed': ${failure.message}",
                    )
                }
                for (line in lines) {
                    val kv = parseCfgKeyValue(line) ?: continue
                    result[kv.first] = kv.second
                }
            } else {
                val kv = parseCfgKeyValue(trimmed) ?: run {
                    return ConditionalCompilationConfigResult.Invalid(
                        "Invalid --cfg entry '$entry': expected key=value or @file",
                    )
                }
                result[kv.first] = kv.second
            }
        }
        return ConditionalCompilationConfigResult.Valid(result.toMap())
    }

    /**
     * 解析单个 `key=value` 条目；格式不合法时返回 null。
     */
    private fun parseCfgKeyValue(line: String): Pair<String, String>? {
        val separator = line.indexOf('=')
        if (separator <= 0 || separator == line.length - 1) return null
        return line.substring(0, separator).trim() to line.substring(separator + 1).trim()
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
