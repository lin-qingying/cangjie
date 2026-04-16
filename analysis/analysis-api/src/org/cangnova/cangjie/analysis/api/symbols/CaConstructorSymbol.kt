package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.name.ClassId

abstract class  CaConstructorSymbol :   CaFunctionSymbol(), CaTypeParameterOwnerSymbol {
    abstract val isPrimary: Boolean
    final  override val modality: CaSymbolModality get() = withValidityAssertion { CaSymbolModality.FINAL }
    abstract override fun createPointer(): CaSymbolPointer<CaConstructorSymbol>

    abstract val containingClassId: ClassId?
}
