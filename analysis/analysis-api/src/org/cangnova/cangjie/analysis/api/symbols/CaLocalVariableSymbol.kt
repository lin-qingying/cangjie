package org.cangnova.cangjie.analysis.api.symbols

/**
 * 局部变量符号。
 *
 * 表示函数体等执行体内部声明的变量绑定（let/var）。
 * catch、command 和 resource 等专用宿主中的绑定也使用这一符号族。
 */
abstract class CaLocalVariableSymbol : CaVariableSymbol()
