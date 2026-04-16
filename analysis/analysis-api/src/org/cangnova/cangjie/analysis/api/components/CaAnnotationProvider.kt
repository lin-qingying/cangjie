package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

interface CaAnnotationProvider : CaLifetimeOwner {
    val CaDeclarationSymbol.annotations: List<CaAnnotation>
}
