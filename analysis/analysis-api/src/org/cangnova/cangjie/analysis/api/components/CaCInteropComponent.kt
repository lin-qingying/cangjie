package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjElement

interface CaCInteropComponent : CaLifetimeOwner {
    fun CjElement.getInteropInfo(): CaInteropInfo?

    fun CaSymbol.getInteropInfo(): CaInteropInfo?
}
