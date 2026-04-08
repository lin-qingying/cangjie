package org.cangnova.cangjie.analysis.api.session

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.pointers.CaTypePointer

/**
 * 指针恢复辅助入口。
 *
 * 在当前架构阶段，恢复逻辑由指针对象本身决定；后续如果 session 需要参与更多策略，
 * 再把该入口内收回 `CaSession` 正式成员。
 */
fun <S : CaSymbol> CaSession.restoreSymbol(pointer: CaSymbolPointer<S>): S? =
    pointer.restoreSymbol(this)

fun <T : CaType> CaSession.restoreType(pointer: CaTypePointer<T>): T? =
    pointer.restoreType(this)

/**
 * 批量恢复符号指针。
 *
 * 该入口与单指针恢复共享同一 session/lifetime 边界，
 * 但避免调用方在工具层反复手工循环指针集合。
 */
fun <S : CaSymbol> CaSession.restoreSymbols(
    pointers: Collection<CaSymbolPointer<S>>,
): List<S?> = pointers.map { pointer -> pointer.restoreSymbol(this) }

/**
 * 批量恢复类型指针。
 */
fun <T : CaType> CaSession.restoreTypes(
    pointers: Collection<CaTypePointer<T>>,
): List<T?> = pointers.map { pointer -> pointer.restoreType(this) }
