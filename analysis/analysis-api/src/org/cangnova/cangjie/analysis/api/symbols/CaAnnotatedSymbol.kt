package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer

public interface CaAnnotatedSymbol : CaSymbol, CaAnnotated {
    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol>
}
