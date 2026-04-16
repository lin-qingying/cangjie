package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol

abstract class CaClassErrorType : CaErrorType {
    abstract val qualifiers: List<CaClassTypeQualifier>

    abstract val candidateSymbols: Collection<CaClassLikeSymbol>

    abstract override fun createPointer(): CaTypePointer<CaClassErrorType>
}
