package org.cangnova.cangjie.chir.core.expression

interface ChirExpressionDomainHandler<T> {
    fun handleUnary(expression: ChirUnaryExpression): T
    fun handleBinary(expression: ChirBinaryExpression): T
    fun handleMemory(expression: ChirMemoryExpression): T
    fun handleCall(expression: ChirCallExpression): T
    fun handleOthers(expression: ChirOtherExpression): T
}

object ChirExpressionDispatcher {
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
