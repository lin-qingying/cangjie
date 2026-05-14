package org.cangnova.cangjie.analysis.api.symbols

/**
 * 宏声明符号。
 *
 * 仓颉的宏在公开符号层与具名函数共享同一抽象（参数列表、返回类型、注解等）。
 * 区别于普通函数：宏在 MACRO_EXPAND 阶段执行而非运行时调用，
 * 因此调用语义和诊断规则不同，但符号视图仍按命名函数建模。
 */
abstract class CaMacroSymbol : CaNamedFunctionSymbol()
