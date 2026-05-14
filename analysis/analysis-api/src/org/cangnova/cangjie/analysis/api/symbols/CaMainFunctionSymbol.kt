package org.cangnova.cangjie.analysis.api.symbols

/**
 * 程序入口 `main` 函数符号。
 *
 * 仓颉中每个可执行单元最多一个 `main`。
 * 它共享命名函数的全部公开能力，但承担"入口"这一特殊语义角色，
 * 因此从 [CaNamedFunctionSymbol] 派生独立类型，方便诊断、运行配置等上层识别。
 */
abstract class CaMainFunctionSymbol : CaNamedFunctionSymbol()
