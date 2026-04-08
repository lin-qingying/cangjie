package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.phaser.LoggingContext

/**
 * 前端管线执行上下文。
 */
class PipelineContext(
    val configuration: CompilerConfiguration,
) : LoggingContext {
    override var inVerbosePhase: Boolean = false
}
