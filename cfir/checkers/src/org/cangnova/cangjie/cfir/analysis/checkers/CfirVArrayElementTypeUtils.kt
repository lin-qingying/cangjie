package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * VArray 元素类型限制的共享语义。
 *
 * 官方 `TypeCheckType.cpp#CheckVArrayWithRefType` 对 VArray 类型声明和
 * VArray 构造表达式中的类型语法使用同一套规则；CFIR 中这两类入口分别表现为
 * resolved type-ref 与 synthetic builtin function call，因此递归判定必须集中在这里。
 */
context(context: CheckerContext)
internal fun findUnsupportedVArrayElementType(
    type: ConeCangJieType,
    visited: MutableSet<ConeCangJieType> = mutableSetOf(),
): ConeCangJieType? {
    val expandedType = type.fullyExpandedType(context.session)
    if (expandedType is ConeErrorType) return null
    if (!visited.add(expandedType)) return null

    return when (expandedType) {
        is ConeClassLikeType,
        is ConeEnumType,
        is ConeTypeParameterType,
        -> expandedType

        is ConeFunctionType -> expandedType.takeUnless { it.isCFunc }

        is ConeTupleType -> expandedType.elementTypes.firstNotNullOfOrNull {
            findUnsupportedVArrayElementType(it, visited)
        }

        is ConeStructType -> expandedType.structFieldTypes(context.session.typeContext).firstNotNullOfOrNull {
            findUnsupportedVArrayElementType(it, visited)
        }

        is ConePointerType,
        is ConeVArrayType,
        -> null

        else -> null
    }
}

context(context: CheckerContext)
private fun ConeStructType.structFieldTypes(typeContext: ConeTypeContext): List<ConeCangJieType> {
    val struct = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirStruct
        ?: return emptyList()
    val typeParameters = struct.typeParameters
    val substitutor = if (typeParameters.isNotEmpty() && typeArguments.isNotEmpty()) {
        createTypeSubstitutorByTypeConstructor(
            map = typeParameters.zip(typeArguments.map { it.type }).associate { (parameter, argument) ->
                parameter.symbol.toLookupTag() as TypeConstructorMarker to argument
            },
            context = typeContext,
            approximateIntegerLiterals = false,
        )
    } else {
        null
    }

    return struct.declarations.mapNotNull { declaration ->
        val field = declaration as? CfirFieldVariable ?: return@mapNotNull null
        val fieldType = (field.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
        substitutor?.substituteOrSelf(fieldType) ?: fieldType
    }
}
