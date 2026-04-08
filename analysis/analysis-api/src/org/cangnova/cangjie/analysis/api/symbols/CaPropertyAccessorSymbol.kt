package org.cangnova.cangjie.analysis.api.symbols

/**
 * 属性访问器的公开语义视图。
 */
interface CaPropertyAccessorSymbol : CaFunctionSymbol {
    val owningProperty: CaPropertySymbol

    val isDefault: Boolean

    val isGetter: Boolean
}

interface CaPropertyGetterSymbol : CaPropertyAccessorSymbol

interface CaPropertySetterSymbol : CaPropertyAccessorSymbol {
    val parameter: CaValueParameterSymbol
}
