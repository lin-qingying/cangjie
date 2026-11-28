package org.cangnova.cangjie.cfir.expressions

/** 仅穿过命名/inout 语法包装取得原委托调用；可选表达式保留自己的执行边界。 */
tailrec fun CfirStatement.delegatedConstructorCallOrNull(): CfirFunctionCall? = when (this) {
    is CfirNamedArgumentExpression -> expression.delegatedConstructorCallOrNull()
    is CfirInoutArgumentExpression -> expression.delegatedConstructorCallOrNull()
    is CfirFunctionCall -> takeIf { it.origin.isConstructorDelegation }
    else -> null
}

/**
 * 调用实参中的 lambda 身份。只剥仓颉的实参名称/inout 包装；Option、可选链和一般值表达式
 * 保留自己的语义，不因其内部出现 lambda 而成为调用的 postponed lambda。
 */
fun CfirExpression.unwrapAnonymousFunctionExpression(): CfirAnonymousFunctionExpression? {
    var current = this
    while (true) {
        current = when (current) {
            is CfirNamedArgumentExpression -> current.expression
            is CfirInoutArgumentExpression -> current.expression
            else -> return current as? CfirAnonymousFunctionExpression
        }
    }
}
