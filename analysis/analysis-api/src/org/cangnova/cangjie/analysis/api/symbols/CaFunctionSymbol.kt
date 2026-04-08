package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * 函数族公开符号根接口。
 */
interface CaFunctionSymbol : CaCallableSymbol, CaTypeParameterOwnerSymbol, CaValueParameterOwnerSymbol {
    val isStatic: Boolean

    val isConst: Boolean

    /**
     * `mut` 修饰符语义。
     *
     * 它和 let/var 可变性是两套完全不同的语义，不能混用。
     */
    val isMutating: Boolean

    val isOverride: Boolean

    val isOperator: Boolean

    val isUnsafe: Boolean

    val isForeign: Boolean
}

interface CaNamedFunctionSymbol : CaFunctionSymbol, CaNamedSymbol

interface CaMainFunctionSymbol : CaNamedFunctionSymbol

interface CaMacroSymbol : CaNamedFunctionSymbol

interface CaAnonymousFunctionSymbol : CaFunctionSymbol

interface CaConstructorSymbol : CaFunctionSymbol {
    val isPrimary: Boolean

    val containingClassId: ClassId?
}

interface CaFinalizerSymbol : CaFunctionSymbol {
    val containingClassId: ClassId?
}
