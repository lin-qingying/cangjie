package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjFile

interface CaSourceProvider : CaLifetimeOwner {
    fun CaSymbol.getContainingFile(): CjFile?
}
