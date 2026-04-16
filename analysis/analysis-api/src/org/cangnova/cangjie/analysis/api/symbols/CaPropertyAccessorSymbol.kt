package org.cangnova.cangjie.analysis.api.symbols

sealed class CaPropertyAccessorSymbol : CaFunctionSymbol() {
    abstract    val owningProperty: CaPropertySymbol

    abstract val isDefault: Boolean

    abstract  val isGetter: Boolean
}
