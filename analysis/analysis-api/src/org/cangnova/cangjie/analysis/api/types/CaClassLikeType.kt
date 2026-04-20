package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

sealed class CaClassLikeType : CaType {
    abstract val classId: ClassId

    abstract val qualifiers: List<CaResolvedClassTypeQualifier>

    abstract override fun createPointer(): CaTypePointer<CaClassLikeType>

    abstract val typeArguments: List<CaType>

    abstract val symbol: CaClassLikeSymbol?
}
