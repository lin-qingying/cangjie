package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

interface CaVisibilityChecker : CaLifetimeOwner {
    fun CaSymbol.isVisible(): Boolean
}
