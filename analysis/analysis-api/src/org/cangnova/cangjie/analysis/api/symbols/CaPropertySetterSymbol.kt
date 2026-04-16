package org.cangnova.cangjie.analysis.api.symbols

abstract class CaPropertySetterSymbol : CaPropertyAccessorSymbol() {
    abstract val parameter: CaValueParameterSymbol
}
