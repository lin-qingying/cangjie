package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * 具备 `ClassId` 身份的 class-like 符号。
 */
interface CaClassLikeSymbol : CaClassifierSymbol, CaNamedSymbol, CaTypeParameterOwnerSymbol {
    val classId: ClassId?
}
