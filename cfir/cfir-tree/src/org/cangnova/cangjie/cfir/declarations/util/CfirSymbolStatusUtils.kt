package org.cangnova.cangjie.cfir.declarations.util

import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

/** 当前 callable 的 resolved status 是否标记为运算符声明。 */
inline val CfirCallableSymbol<*>.isOperator: Boolean get() = resolvedStatus.isOperator

/** 当前 callable 的 resolved status 是否标记为 `mut` 修饰的可变成员。 */
inline val CfirCallableSymbol<*>.isMut: Boolean get() = resolvedStatus.isMut
