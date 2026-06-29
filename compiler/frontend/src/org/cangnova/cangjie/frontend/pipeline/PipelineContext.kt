package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.phaser.LoggingContext

/**
 * 前端管线执行上下文。
 */
class PipelineContext(
    /**
     * 当前管线执行使用的编译器配置。
     */
    val configuration: CompilerConfiguration,
) : LoggingContext {
    /**
     * 当前是否处于 verbose 阶段输出模式。
     */
    override var inVerbosePhase: Boolean = false
}
