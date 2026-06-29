package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.phaser.Action
import org.cangnova.cangjie.phaser.ActionState

/**
 * 前端管线中检查编译错误并中断后续阶段的动作基类。
 */
abstract class CheckCompilationErrors : Action<PipelineArtifact, PipelineContext> {

    /**
     * 检查诊断收集器和消息收集器的管线动作。
     */
    object CheckDiagnosticCollector : CheckCompilationErrors() {
        /**
         * 在阶段结束后检查当前配置是否已经出现错误。
         */
        override fun invoke(
            state: ActionState,
            output: PipelineArtifact,
            c: PipelineContext,
        ) {
            if (checkHasErrors(output.configuration)) {
                throw PipelineStepException()
            }
        }

        /**
         * 判断配置中累计的诊断或消息是否包含错误。
         */
        fun checkHasErrors(configuration: CompilerConfiguration): Boolean =
            configuration.diagnosticsCollector.hasErrors ||
                configuration.messageCollector.hasErrors()

        /**
         * 检查错误并在发现错误时转发到消息收集器。
         */
        fun checkHasErrorsAndReportToMessageCollector(configuration: CompilerConfiguration): Boolean {
            if (checkHasErrors(configuration)) {
                reportToMessageCollector(configuration)
                return true
            }
            return false
        }

        /**
         * 将诊断收集器中的错误报告到消息收集器。
         */
        fun reportToMessageCollector(configuration: CompilerConfiguration) {
//          TODO 转发到 消息收集器
        }
    }
}
