package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

/**
 * 拥有 `this` 接收者语义的符号基类。
 *
 * 该层对齐 Kotlin FIR 的 `FirThisOwnerSymbol`，用于 class-like、extend 等可以成为 `this`
 * owner 的声明符号。
 */
sealed class CfirThisOwnerSymbol<out D : CfirDeclaration> : CfirBasedSymbol<D>()
