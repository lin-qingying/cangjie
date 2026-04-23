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
fun <S : CaSymbol> CaSession.restoreSymbol(pointer: CaSymbolPointer<S>): S? =
    pointer.restoreSymbol(this)

@OptIn(CaImplementationDetail::class)
fun <T : CaType> CaSession.restoreType(pointer: CaTypePointer<T>): T? =
    pointer.restore(this)

fun <S : CaSymbol> CaSession.restoreSymbols(
    pointers: Collection<CaSymbolPointer<S>>,
): List<S?> = pointers.map { pointer -> pointer.restoreSymbol(this) }

@OptIn(CaImplementationDetail::class)
fun <T : CaType> CaSession.restoreTypes(
    pointers: Collection<CaTypePointer<T>>,
): List<T?> = pointers.map { pointer -> pointer.restore(this) }
