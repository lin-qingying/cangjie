package org.cangnova.cangjie.cli.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.phaser.LoggingContext

/**
 * Pipeline 执行上下文
 */
class PipelineContext(
    val configuration: CompilerConfiguration
) : LoggingContext {
    override var inVerbosePhase: Boolean = false
}
