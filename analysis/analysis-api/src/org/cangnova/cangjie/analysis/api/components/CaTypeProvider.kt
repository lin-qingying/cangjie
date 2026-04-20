package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

interface CaTypeProvider : CaLifetimeOwner {
    val CaClassLikeSymbol.defaultType: CaType
}
