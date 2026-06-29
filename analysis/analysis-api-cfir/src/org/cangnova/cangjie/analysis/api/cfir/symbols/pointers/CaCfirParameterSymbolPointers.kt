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
    /**
     * 类型参数 owner 的公开符号 pointer。
     */
    private val ownerPointer: CaSymbolPointer<CaSymbol>,
    /**
     * 类型参数名称，用于恢复后的校验。
     */
    private val parameterName: Name,
    /**
     * 类型参数在 owner 类型参数列表中的稳定下标。
     */
    private val parameterIndex: Int,
) : CaCfirSymbolPointerBase<CaTypeParameterSymbol>() {
    /**
     * 通过 owner pointer 和稳定下标恢复类型参数符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaTypeParameterSymbol? {
        val owner = ownerPointer.restoreSymbol(session) as? CaTypeParameterOwnerSymbol ?: return null
        return owner.typeParameters.getOrNull(parameterIndex)?.takeIf { parameter -> parameter.name == parameterName }
    }
}

/**
 * 值参数符号 pointer。
 */
internal class CaCfirValueParameterSymbolPointer(
    /**
     * 值参数 owner 的公开符号 pointer。
     */
    private val ownerPointer: CaSymbolPointer<CaSymbol>,
    /**
     * 值参数名称，用于恢复后的校验。
     */
    private val parameterName: Name,
    /**
     * 值参数在 owner 值参数列表中的稳定下标。
     */
    private val parameterIndex: Int,
) : CaCfirSymbolPointerBase<CaValueParameterSymbol>() {
    /**
     * 通过 owner pointer 和稳定下标恢复值参数符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaValueParameterSymbol? {
        val owner = ownerPointer.restoreSymbol(session) as? CaValueParameterOwnerSymbol ?: return null
        return owner.valueParameters.getOrNull(parameterIndex)?.takeIf { parameter -> parameter.name == parameterName }
    }
}
