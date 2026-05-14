package org.cangnova.cangjie.analysis.api.session

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer

/**
 * pointer 恢复的 session 包入口。
 *
 * testFixtures 与上层调用约定从 `analysis.api.session` 导入这些扩展，
 * 这里保留这层 facade，避免把调用方绑死到 `CaSession.kt` 的文件落位。
 */

/**
 * 在当前会话中按 [pointer] 恢复单个 symbol;无法恢复时返回 `null`。
 *
 * 与 `CaSession.restoreSymbol` 同义,仅在 `analysis.api.session` 包级别再暴露一次。
 */
fun <S : CaSymbol> CaSession.restoreSymbol(pointer: CaSymbolPointer<S>): S? =
    pointer.restoreSymbol(this)

/**
 * 在当前会话中按 [pointer] 恢复单个 type;无法恢复时返回 `null`。
 *
 * 与 `CaSession.restoreType` 同义,仅在 `analysis.api.session` 包级别再暴露一次。
 */
@OptIn(CaImplementationDetail::class)
fun <T : CaType> CaSession.restoreType(pointer: CaTypePointer<T>): T? =
    pointer.restore(this)

/**
 * 批量恢复 symbol 指针,无法恢复的位置填 `null`。
 *
 * 与 `CaSession.restoreSymbols` 同义,仅在 `analysis.api.session` 包级别再暴露一次。
 */
fun <S : CaSymbol> CaSession.restoreSymbols(
    pointers: Collection<CaSymbolPointer<S>>,
): List<S?> = pointers.map { pointer -> pointer.restoreSymbol(this) }

/**
 * 批量恢复 type 指针,无法恢复的位置填 `null`。
 *
 * 与 `CaSession.restoreTypes` 同义,仅在 `analysis.api.session` 包级别再暴露一次。
 */
@OptIn(CaImplementationDetail::class)
fun <T : CaType> CaSession.restoreTypes(
    pointers: Collection<CaTypePointer<T>>,
): List<T?> = pointers.map { pointer -> pointer.restore(this) }
