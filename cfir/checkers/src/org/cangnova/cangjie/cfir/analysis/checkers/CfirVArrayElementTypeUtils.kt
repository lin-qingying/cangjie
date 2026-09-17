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
import org.cangnova.cangjie.cfir.types.StdlibClassIds
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
        is ConeStructType -> when (expandedType.classId) {
            // 官方把标准库 Array 作为 IsStructArray 直接拒绝；String 虽以 struct
            // 形式进入 Cone，但其官方内建布局同样不是可逐字段下沉的普通用户 struct。
            StdlibClassIds.Array,
            StdlibClassIds.String,
            -> expandedType

            else -> expandedType.structFieldTypes(context.session.typeContext).firstNotNullOfOrNull {
                findUnsupportedVArrayElementType(it, visited)
            }
        }

        is ConeClassLikeType,
        is ConeEnumType,
        is ConeTypeParameterType,
        -> expandedType

        is ConeFunctionType -> expandedType.takeUnless { it.isCFunc }

        is ConeTupleType -> expandedType.elementTypes.firstNotNullOfOrNull {
            findUnsupportedVArrayElementType(it, visited)
        }

        is ConePointerType,
        is ConeVArrayType,
        -> null

        else -> null
    }
}

/**
 * 读取 struct 字段的实际类型，并按当前 struct 类型实参替换字段类型中的类型参数。
 *
 * VArray 元素限制需要递归进入 struct 字段；泛型 struct 必须使用实例化后的字段类型，
 * 否则会把声明侧类型参数误判为实际元素类型。
 */
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
        // 官方 CheckVArrayWithRefType 只检查 struct 的实例字段；static 字段不属于
        // 每个 struct 值的存储布局，不能把它们的引用类型传递到 VArray 元素判定中。
        if (field.status.isStatic) return@mapNotNull null
        val fieldType = (field.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
        substitutor?.substituteOrSelf(fieldType) ?: fieldType
    }
}
