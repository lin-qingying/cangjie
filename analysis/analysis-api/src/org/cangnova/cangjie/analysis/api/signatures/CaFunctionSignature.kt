package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol

/**
 * 函数族 use-site 签名。
 *
 * 该层对齐 Kotlin `KaFunctionSignature`：
 * 它不新增额外状态，只是把底层 symbol 族约束收窄到 `CaFunctionSymbol`，
 * 以便 `asSignature()` / `substitute()` 在公开 API 上保留精确返回类型。
 */
interface CaFunctionSignature<out S : CaFunctionSymbol> : CaSignature<S>
