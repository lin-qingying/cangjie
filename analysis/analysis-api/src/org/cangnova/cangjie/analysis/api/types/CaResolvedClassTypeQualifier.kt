package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol

interface CaResolvedClassTypeQualifier : CaClassTypeQualifier {
    val symbol: CaClassifierSymbol
}
