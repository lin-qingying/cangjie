package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol

/**
 * 变量族 use-site 签名。
 *
 * 仓颉当前公开变量族包括属性、字段、局部变量、参数等 callable 叶子，
 * 这里统一复用 `CaSignature` 的通用签名语义，并把底层 symbol 族约束到 `CaVariableSymbol`。
 */
interface CaVariableSignature<out S : CaVariableSymbol> : CaSignature<S>
