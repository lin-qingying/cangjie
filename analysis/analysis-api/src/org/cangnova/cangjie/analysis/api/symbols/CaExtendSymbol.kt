package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId

/**
 * extend 声明的公开语义视图。
 */
interface CaExtendSymbol : CaDeclarationSymbol, CaDeclarationContainerSymbol, CaTypeParameterOwnerSymbol {
    val extendId: String

    val targetClassId: ClassId?

    val extendedType: CaType

    val superTypes: List<CaType>
}
