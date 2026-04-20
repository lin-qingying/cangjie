package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer

/**
 * CFIR 符号指针的内部基类。
 *
 * Kotlin FIR 侧按符号种类拆出专用 pointer，这里也保持同样的组织方式。
 * 基类只负责把 `CaSession` 归一化为 `CaCfirSession`，不再引入统一 restore-key 协议。
 */
internal abstract class CaCfirSymbolPointerBase<out S : CaSymbol> : CaSymbolPointer<S> {
    protected fun restoreSession(session: CaSession): CaCfirSession? = session as? CaCfirSession
}

internal fun <S : CaSymbol> Class<S>.castOrNull(symbol: CaSymbol?): S? =
    if (symbol != null && isInstance(symbol)) cast(symbol) else null
