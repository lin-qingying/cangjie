package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.collectUpperBounds
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.hasSupertypeWithGivenClassId
import org.cangnova.cangjie.cfir.types.typeContext

internal fun prepareArgumentType(argumentType: ConeCangJieType, session: CfirSession): ConeCangJieType {
    return argumentType.fullyExpandedType(session)
}


/**
 * 在仓颉调用检查里，如果实参本身是类型参数，而该类型参数的某个上界已经具备期望类型的构造器，
 * 则可以先把实参视为该上界参与约束，而不是直接拿类型参数本身做 subtype 检查。
 *
 * 例如：
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<Int64>> foo(x: X) {
 *     val xr = bar(x)
 * }
 *
 * 这里会把 `x` 临时视为其上界 `Inv<Int64>`。
 * 仓颉当前没有柔性类型，也没有 Kotlin 式显式 in/out 变型，
 * 因而这里只做上界替换，不再引入 captured type 中间表示。
 *
 * 这等价于：
 * fun <X : Inv<Int64>> foo(x: X) {
 *     val inv: Inv<Int64> = x
 *     val xr = bar(inv)
 * }
 */
internal fun substituteTypeParameterUpperBoundIfNeeded(
    argumentType: ConeCangJieType,
    expectedType: ConeCangJieType,
    session: CfirSession
): ConeCangJieType {
    val expectedTypeClassId = expectedType.classIdOrPrimitiveClassId ?: return argumentType
    val context = session.typeContext
    val chosenSupertype = when (argumentType) {
        is ConeTypeParameterType -> argumentType.collectUpperBounds(context)
            .singleOrNull { it.hasSupertypeWithGivenClassId(expectedTypeClassId, context) }

        is ConeTypeVariableType -> {
            val originalTypeParameter = argumentType.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
                ?: return argumentType
            ConeTypeParameterTypeImpl(originalTypeParameter, argumentType.attributes)
                .collectUpperBounds(context)
                .singleOrNull { it.hasSupertypeWithGivenClassId(expectedTypeClassId, context) }
                ?: ConeTypeParameterTypeImpl(originalTypeParameter, argumentType.attributes)
        }

        else -> null
    } ?: return argumentType
    return chosenSupertype
}

internal fun normalizeTypeForCompatibilityCheck(type: ConeCangJieType): ConeCangJieType {
    return when (type) {
        is ConeTypeVariableType -> {
            val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
            if (originalTypeParameter != null) {
                ConeTypeParameterTypeImpl(originalTypeParameter, type.attributes)
            } else {
                type
            }
        }

        else -> type
    }
}

fun CfirExpression.getExpectedType(
    session: CfirSession,
    parameter: CfirValueParameter
): ConeCangJieType {
//    val shouldUnwrapVarargType = when (this) {
//        is CfirSpreadArgumentExpression, is CfirNamedArgumentExpression -> false
//        else -> parameter.isVararg
//    }

    val expectedType =
//        if (shouldUnwrapVarargType) {
//        parameter.returnTypeRef.coneType.varargElementType()
//    } else {
        parameter.returnTypeRef.coneType
//    }
    return expectedType
//    if (!session.functionTypeService.hasExtensionKinds()) return expectedType
//    return FunctionTypeKindSubstitutor(session).substituteOrSelf(expectedType)
}
