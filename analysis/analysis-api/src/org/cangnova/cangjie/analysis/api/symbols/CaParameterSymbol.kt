package org.cangnova.cangjie.analysis.api.symbols

/**
 * 参数符号的公共父类型。
 *
 * 用 `sealed` 收敛所有参数形态。当前仅有 [CaValueParameterSymbol] 这一类；
 * 设计上预留对未来其他参数形态（如类型类约束参数等）扩展的入口。
 *
 * 参数本质上是 [CaVariableSymbol] 的一种：参数在作用域内表现为一个可读变量绑定。
 */
sealed class CaParameterSymbol : CaVariableSymbol()
