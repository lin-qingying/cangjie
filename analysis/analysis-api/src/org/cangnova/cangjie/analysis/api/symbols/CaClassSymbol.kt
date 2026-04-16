package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * class/interface/struct/enum 这类真实类型声明的公开语义视图。
 */
interface CaClassSymbol : CaClassLikeSymbol, CaDeclarationContainerSymbol {
    val classKind: CaClassKind

    val superTypes: List<CaType>
}
