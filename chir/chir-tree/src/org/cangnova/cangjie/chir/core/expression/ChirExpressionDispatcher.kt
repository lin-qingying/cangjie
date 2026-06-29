package org.cangnova.cangjie.chir.core.expression

/**
 * 按表达式语义域分发的处理器接口。
 */
interface ChirExpressionDomainHandler<T> {
    /**
     * 处理一元表达式。
     */
    fun handleUnary(expression: ChirUnaryExpression): T

    /**
     * 处理二元表达式。
     */
    fun handleBinary(expression: ChirBinaryExpression): T

    /**
     * 处理内存表达式。
     */
    fun handleMemory(expression: ChirMemoryExpression): T

    /**
     * 处理调用表达式。
     */
    fun handleCall(expression: ChirCallExpression): T

    /**
     * 处理其他表达式。
     */
    fun handleOthers(expression: ChirOtherExpression): T
}

/**
 * CHIR 表达式分发器。
 */
object ChirExpressionDispatcher {
    /**
     * 按 [expression] 具体类型调用 [handler] 中对应处理方法。
     */
    fun <T> dispatch(
        expression: ChirExpression,
        handler: ChirExpressionDomainHandler<T>,
    ): T {
        return when (expression) {
            is ChirUnaryExpression -> handler.handleUnary(expression)
            is ChirBinaryExpression -> handler.handleBinary(expression)
            is ChirMemoryExpression -> handler.handleMemory(expression)
            is ChirCallExpression -> handler.handleCall(expression)
            is ChirOtherExpression -> handler.handleOthers(expression)
            else -> error("unsupported expression type: ${expression::class.qualifiedName}")
        }
    }
}
