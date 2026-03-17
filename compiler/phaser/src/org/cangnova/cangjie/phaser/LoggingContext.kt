package org.cangnova.cangjie.phaser

/**
 * 日志上下文（对齐 K2 的 LoggingContext）
 */
interface LoggingContext {
    /**
     * 是否在详细阶段中
     */
    var inVerbosePhase: Boolean

    /**
     * 如果 inVerbosePhase 为 true，打印消息到标准错误流
     */
    fun log(message: () -> String) {
        if (inVerbosePhase) {
            System.err.println(message())
        }
    }
}
