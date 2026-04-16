package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * typealias 的公开语义视图。
 */
interface CaTypeAliasSymbol : CaClassLikeSymbol {
    val expandedType: CaType
}
