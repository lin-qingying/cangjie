package org.cangnova.cangjie.cli.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.phaser.Action
import org.cangnova.cangjie.phaser.ActionState

abstract class CheckCompilationErrors : Action<PipelineArtifact, PipelineContext> {

    object CheckDiagnosticCollector : CheckCompilationErrors() {
        override fun invoke(
            state: ActionState,
            output: PipelineArtifact,
            c: PipelineContext,
        ) {
            if (checkHasErrors(output.configuration)) {
                throw PipelineStepException()
            }
        }

        fun checkHasErrors(configuration: CompilerConfiguration): Boolean =
            configuration.diagnosticsCollector.hasErrors ||
                    configuration.messageCollector.hasErrors()

        fun checkHasErrorsAndReportToMessageCollector(configuration: CompilerConfiguration): Boolean {
            if (checkHasErrors(configuration)) {
                reportToMessageCollector(configuration)
                return true
            }
            return false
        }

        fun reportToMessageCollector(configuration: CompilerConfiguration) {
//          TODO 转发到 消息收集器
        }
    }
}