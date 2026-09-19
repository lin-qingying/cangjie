package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.config.ApiVersion
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
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
        if (!configuration.applyLanguageVersionArgument(arguments)) return false
        val input = ArgumentsPipelineArtifact(arguments, configuration)
        return runPhasedPipeline(input)
    }

    /**
     * 将生产参数中的 language-version 接入唯一的配置 settings owner。
     *
     * Kotlin 的 CLI configurator 在进入 FIR session 前完成同一转换；这里保留
     * 已注入的显式 feature 与 analysis flags，只替换命令行明确覆盖的语言/API
     * 版本，避免前端阶段再按参数文本分叉。
     */
    private fun CompilerConfiguration.applyLanguageVersionArgument(arguments: A): Boolean {
        val rawVersion = arguments.languageVersion ?: return true
        val languageVersion = try {
            LanguageVersion.parse(rawVersion)
        } catch (error: IllegalStateException) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Invalid language version '$rawVersion': ${error.message ?: "unknown version"}",
            )
            return false
        }

        if (languageVersion.isUnsupported) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Language version '${languageVersion.versionString}' is not supported",
            )
            return false
        }

        val current = languageVersionSettings
        val requestedApi = ApiVersion.createByLanguageVersion(languageVersion)
        val effectiveApi = if (current.apiVersion <= requestedApi || arguments.autoAdvanceApiVersion) {
            minOf(current.apiVersion, requestedApi)
        } else {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "API version '${current.apiVersion.versionString}' cannot be used with " +
                    "language version '${languageVersion.versionString}'",
            )
            return false
        }
        val versioned = LanguageVersionSettingsImpl(
            languageVersion = languageVersion,
            apiVersion = effectiveApi,
            specificFeatures = current.getCustomizedLanguageFeatures(),
        )
        languageVersionSettings = object : LanguageVersionSettings by versioned {
            override fun <T> getFlag(flag: org.cangnova.cangjie.AnalysisFlag<T>): T = current.getFlag(flag)
        }
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
