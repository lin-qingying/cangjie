package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer

/**
 * 属性访问器 pointer。
 *
 * Kotlin FIR 侧同样通过 owning property 恢复 getter / setter；
 * 这里保持同样思路，不再把访问器塞回统一 restore-key 分派。
 */
internal class CaCfirPropertyGetterSymbolPointer(
    private val ownerPointer: CaSymbolPointer<CaPropertySymbol>,
) : CaCfirSymbolPointerBase<CaPropertyGetterSymbol>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPropertyGetterSymbol? =
        ownerPointer.restoreSymbol(session)?.getter
}

internal class CaCfirPropertySetterSymbolPointer(
    private val ownerPointer: CaSymbolPointer<CaPropertySymbol>,
) : CaCfirSymbolPointerBase<CaPropertySetterSymbol>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPropertySetterSymbol? =
        ownerPointer.restoreSymbol(session)?.setter
}
