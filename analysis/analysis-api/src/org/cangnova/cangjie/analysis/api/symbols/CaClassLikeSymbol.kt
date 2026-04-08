package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId

/**
 * 分类器符号根接口。
 */
interface CaClassifierSymbol : CaDeclarationSymbol

/**
 * 具备 `ClassId` 身份的 class-like 符号。
 */
interface CaClassLikeSymbol : CaClassifierSymbol, CaNamedSymbol, CaTypeParameterOwnerSymbol {
    val classId: ClassId?
}

/**
 * class/interface/struct/enum 这类真实类型声明的公开语义视图。
 */
interface CaClassSymbol : CaClassLikeSymbol, CaDeclarationContainerSymbol {
    val classKind: CaClassKind

    val superTypes: List<CaType>
}

enum class CaClassKind {
    CLASS,
    INTERFACE,
    STRUCT,
    ENUM,
}

/**
 * typealias 的公开语义视图。
 */
interface CaTypeAliasSymbol : CaClassLikeSymbol {
    val expandedType: CaType
}

/**
 * 类型参数的公开语义视图。
 */
interface CaTypeParameterSymbol : CaClassifierSymbol, CaNamedSymbol {
    val upperBounds: List<CaType>
}

/**
 * extend 声明的公开语义视图。
 */
interface CaExtendSymbol : CaDeclarationSymbol, CaDeclarationContainerSymbol, CaTypeParameterOwnerSymbol {
    val extendId: String

    val targetClassId: ClassId?

    val extendedType: CaType

    val superTypes: List<CaType>
}
