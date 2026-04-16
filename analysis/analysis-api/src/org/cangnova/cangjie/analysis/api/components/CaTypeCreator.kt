package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.name.ClassId

interface CaTypeCreator : CaLifetimeOwner {
    fun buildClassLikeType(
        classId: ClassId,
        typeArguments: List<CaType> = emptyList(),
    ): CaClassLikeType

    fun buildClassLikeType(
        symbol: CaClassLikeSymbol,
        typeArguments: List<CaType> = emptyList(),
    ): CaClassLikeType

    fun buildFunctionType(
        parameterTypes: List<CaType>,
        returnType: CaType,
        isCFunction: Boolean = false,
        isClosureType: Boolean = false,
        hasVariableLengthArgument: Boolean = false,
    ): CaFunctionType

    fun buildTupleType(
        elementTypes: List<CaType>,
    ): CaTupleType

    fun buildIntersectionType(
        conjuncts: List<CaType>,
    ): CaIntersectionType

    fun buildUnionType(
        alternatives: Collection<CaType>,
    ): CaUnionType
}
