package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol

/**
 * 变量族公开符号根接口。
 *
 * `isVal` 只表达 let/var 这类声明侧可变性，不承载 `mut` 修饰符语义。
 */
interface CaVariableSymbol : CaCallableSymbol, CaNamedSymbol {
    val isVal: Boolean
}

interface CaPropertySymbol : CaVariableSymbol, CaTypeParameterOwnerSymbol, CaDeclarationContainerSymbol {
    val isStatic: Boolean

    val isConst: Boolean

    val isMutating: Boolean

    val isOverride: Boolean

    val isUnsafe: Boolean

    val isForeign: Boolean

    val getter: CaPropertyGetterSymbol?

    val setter: CaPropertySetterSymbol?
}

interface CaFieldSymbol : CaVariableSymbol {
    val isStatic: Boolean

    val isConst: Boolean
}

interface CaLocalVariableSymbol : CaVariableSymbol

interface CaPatternVariableSymbol : CaLocalVariableSymbol

interface CaPatternBindingSymbol : CaLocalVariableSymbol

interface CaEnumEntrySymbol : CaVariableSymbol

interface CaParameterSymbol : CaVariableSymbol

interface CaValueParameterSymbol : CaParameterSymbol {
    val isNamed: Boolean

    val hasDefaultValue: Boolean
}
