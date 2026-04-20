package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.name.Name

/**
 * 类型参数 / 值参数 pointer。
 *
 * 这里对齐 Kotlin 的恢复策略：owner pointer + 稳定序号 + 名字校验。
 * 名字只用于额外一致性校验，不再单独承担身份职责。
 */
internal class CaCfirTypeParameterSymbolPointer(
    private val ownerPointer: CaSymbolPointer<CaSymbol>,
    private val parameterName: Name,
    private val parameterIndex: Int,
) : CaCfirSymbolPointerBase<CaTypeParameterSymbol>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaTypeParameterSymbol? {
        val owner = ownerPointer.restoreSymbol(session) as? CaTypeParameterOwnerSymbol ?: return null
        return owner.typeParameters.getOrNull(parameterIndex)?.takeIf { parameter -> parameter.name == parameterName }
    }
}

internal class CaCfirValueParameterSymbolPointer(
    private val ownerPointer: CaSymbolPointer<CaSymbol>,
    private val parameterName: Name,
    private val parameterIndex: Int,
) : CaCfirSymbolPointerBase<CaValueParameterSymbol>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaValueParameterSymbol? {
        val owner = ownerPointer.restoreSymbol(session) as? CaValueParameterOwnerSymbol ?: return null
        return owner.valueParameters.getOrNull(parameterIndex)?.takeIf { parameter -> parameter.name == parameterName }
    }
}
