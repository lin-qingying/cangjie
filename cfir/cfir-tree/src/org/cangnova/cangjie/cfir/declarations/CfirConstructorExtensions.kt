package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression

/**
 * 构造器 body 中第一条委托构造调用（`this(...)` / `super(...)`）。
 *
 * 对齐 Kotlin `FirConstructor.delegatedConstructor`：解析器把委托调用放在 body 的第一条语句，
 * 可能被语句包装层包裹。没有委托调用时返回 `null`。
 */
val CfirConstructor.delegatedConstructor: CfirFunctionCall?
    get() {
        val statement = body?.statements?.firstOrNull() ?: return null
        val expression = when (statement) {
            is CfirWrappedExpression -> statement.expression
            is CfirExpression -> statement
            else -> return null
        }
        val call = expression as? CfirFunctionCall ?: return null
        return call.takeIf { it.origin.isConstructorDelegation }
    }
