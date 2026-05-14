package org.cangnova.cangjie.analysis.api.symbols

/**
 * 匿名函数（function literal）的公开符号。
 *
 * 匿名函数不具备稳定名字，因此不实现 `CaNamedSymbol`，
 * 也不参与基于 callable id 的全局查找，仅在所在作用域内可见。
 */
abstract class CaAnonymousFunctionSymbol : CaFunctionSymbol()
