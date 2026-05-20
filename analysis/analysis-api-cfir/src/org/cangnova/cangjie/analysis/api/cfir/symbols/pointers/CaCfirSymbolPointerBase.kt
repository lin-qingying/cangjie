package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.impl.base.symbols.pointers.CaBaseCachedSymbolPointer
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

/**
 * 对齐 Kotlin `KaBaseCachedSymbolPointer` 的仓颉侧 cached pointer 基座。
 *
 * 这里把“优先返回原始 symbol、否则走后端恢复”的协议统一收敛，
 * 让 top-level/member 等分层 pointer 只关心候选选择职责。
 */
internal abstract class CaCfirCachedSymbolPointer<S : CaSymbol>(
    originalSymbol: S?,
) : CaBaseCachedSymbolPointer<S>(originalSymbol) {
    protected abstract override fun restoreIfNotCached(session: CaSession): S?
}

internal fun <S : CaSymbol> Class<S>.castOrNull(symbol: CaSymbol?): S? =
    if (symbol != null && isInstance(symbol)) cast(symbol) else null
