package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer

/**
 * 函数族公开符号根接口。
 */
public sealed class CaFunctionSymbol : CaCallableSymbol(), CaTypeParameterOwnerSymbol, CaValueParameterOwnerSymbol {
    abstract val isStatic: Boolean

    abstract val isConst: Boolean
    abstract override fun createPointer(): CaSymbolPointer<CaFunctionSymbol>

    /**
     * `mut` 修饰符语义。
     *
     * 它和 let/var 可变性是两套完全不同的语义，不能混用。
     */
    abstract  val isMutating: Boolean

    abstract val isOverride: Boolean

    abstract  val isOperator: Boolean

    abstract  val isUnsafe: Boolean

    abstract   val isForeign: Boolean
}
