package org.cangnova.cangjie.analysis.api.symbols

/**
 * 局部变量符号。
 *
 * 表示函数体等执行体内部声明的变量绑定（let/var）。
 * 它是 [CaPatternBindingSymbol] / [CaPatternVariableSymbol] 等模式相关
 * 局部绑定符号的公共父类型。
 */
abstract class CaLocalVariableSymbol : CaVariableSymbol()
