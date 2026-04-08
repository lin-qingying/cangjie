package org.cangnova.cangjie.analysis.api.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * 符号指针。
 *
 * 指针是跨 `analyze {}` 生命周期边界传递符号的唯一合法方式。
 */
interface CaSymbolPointer<out S : CaSymbol> {
    fun restoreSymbol(session: CaSession): S?
}
