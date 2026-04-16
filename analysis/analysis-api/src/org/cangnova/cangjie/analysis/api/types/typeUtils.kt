package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol

val CaType.symbol: CaClassLikeSymbol?
    get() = (this as? CaClassLikeType)?.symbol
