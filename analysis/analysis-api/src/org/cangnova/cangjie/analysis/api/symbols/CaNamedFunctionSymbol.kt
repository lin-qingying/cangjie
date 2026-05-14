package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

/**
 * 具名函数符号。
 *
 * 与匿名函数 ([CaAnonymousFunctionSymbol]) 相对：
 * - 有稳定名字（通过 [CaNamedSymbol]）；
 * - 可在源码中独立声明，且可参与基于名字/callable id 的查找。
 *
 * 进一步派生出 [CaMainFunctionSymbol]（程序入口）与 [CaMacroSymbol]（宏声明）等特化形态。
 */
abstract class CaNamedFunctionSymbol : CaFunctionSymbol(), CaNamedSymbol
