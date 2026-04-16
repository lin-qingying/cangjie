package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.name.Name

abstract class CaTypeParameterType : CaType {
    abstract val name: Name

    abstract val symbol: CaTypeParameterSymbol

    abstract override fun createPointer(): CaTypePointer<CaTypeParameterType>
}
