package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer

/**
 * 指针协议。
 *
 * 所有跨 `analyze {}` 传递的 symbol 都必须先降格为 pointer，
 * 避免直接泄漏 session 内部对象。
 */
interface CaSymbolInformationProvider : CaLifetimeOwner {
    fun CaSymbol.createPointer(): CaSymbolPointer<CaSymbol>
}
