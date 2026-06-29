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
    /**
     * getter 所属属性的 pointer。
     */
    private val ownerPointer: CaSymbolPointer<CaPropertySymbol>,
) : CaCfirSymbolPointerBase<CaPropertyGetterSymbol>() {
    /**
     * 通过所属属性恢复 getter 符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPropertyGetterSymbol? =
        ownerPointer.restoreSymbol(session)?.getter
}

/**
 * 属性 setter 符号 pointer。
 */
internal class CaCfirPropertySetterSymbolPointer(
    /**
     * setter 所属属性的 pointer。
     */
    private val ownerPointer: CaSymbolPointer<CaPropertySymbol>,
) : CaCfirSymbolPointerBase<CaPropertySetterSymbol>() {
    /**
     * 通过所属属性恢复 setter 符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPropertySetterSymbol? =
        ownerPointer.restoreSymbol(session)?.setter
}
