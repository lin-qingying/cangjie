package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.declaredUpperBoundConeTypeOrNull
import org.cangnova.cangjie.cfir.types.declaredUpperBoundRefsAfterTypeResolve
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.hasInvalidDeclaredUpperBounds
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 官方 FilterShadowedFunc 的词法函数签名比较。
 *
 * 遮蔽只比较是否为泛型函数及参数类型，不比较返回类型、类型参数个数、参数名或默认值。
 * 该判断先于候选适用性检查，不能因内层函数返回类型不兼容而重新引入外层同签名函数。
 */
internal fun CfirNamedFunction.hasSameParameterSignatureForScopeShadowing(
    other: CfirNamedFunction,
    session: CfirSession,
): Boolean {
    if (typeParameters.isEmpty() != other.typeParameters.isEmpty()) return false
    if (valueParameters.size != other.valueParameters.size) return false
    val comparison = FunctionParameterTypeComparison(session)
    return valueParameters.zip(other.valueParameters).all { (first, second) ->
        val firstType = first.returnTypeRef.coneTypeOrNull ?: return@all false
        val secondType = second.returnTypeRef.coneTypeOrNull ?: return@all false
        comparison.areIdentical(firstType, secondType)
    }
}

/**
 * 对齐官方 IsFuncParameterTypesIdentical / CheckTypeCompatibility 的 IDENTICAL 分支。
 * 普通参数中的类型参数保留符号身份；enum 类型实参允许比较类型参数的声明上界，
 * 这是官方 enum 签名规则，不能用 override 的按位置重命名规则替代。
 */
private class FunctionParameterTypeComparison(private val session: CfirSession) {
    private val typeCheckerState = session.typeContext.newTypeCheckerState(
        errorTypesEqualToAnything = false,
        stubTypesEqualToAnything = false,
    )
    private val activePairs = hashSetOf<Triple<ConeCangJieType, ConeCangJieType, Boolean>>()

    fun areIdentical(
        first: ConeCangJieType,
        second: ConeCangJieType,
        compareGenericBounds: Boolean = false,
    ): Boolean {
        val left = first.fullyExpandedType(session)
        val right = second.fullyExpandedType(session)
        if (left.containsErrorType() || right.containsErrorType()) return false
        if (AbstractTypeChecker.equalTypes(typeCheckerState, left, right)) return true
        if (AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(typeCheckerState, left, right)) return false

        // 递归结构按正在比较的类型对闭合，不重新进入函数体或启动候选推断。
        val pair = Triple(left, right, compareGenericBounds)
        if (!activePairs.add(pair)) return true
        try {
            if (compareGenericBounds && left is ConeTypeParameterType && right is ConeTypeParameterType) {
                return haveIdenticalBounds(left, right)
            }
            if (!left.isEnumSignatureType() || !right.isEnumSignatureType()) return false
            left as ConeLookupTagBasedType
            right as ConeLookupTagBasedType
            if (left.expandedClassIdOrPrimitiveClassId != right.expandedClassIdOrPrimitiveClassId) return false
            if (left.typeArguments.size != right.typeArguments.size) return false
            return left.typeArguments.zip(right.typeArguments).all { (firstArgument, secondArgument) ->
                areIdentical(firstArgument.type, secondArgument.type, compareGenericBounds = true)
            }
        } finally {
            activePairs.remove(pair)
        }
    }

    private fun haveIdenticalBounds(first: ConeTypeParameterType, second: ConeTypeParameterType): Boolean {
        if (first.hasInvalidDeclaredUpperBounds(session) || second.hasInvalidDeclaredUpperBounds(session)) return false
        val firstBounds = first.lookupTag.declaredUpperBoundRefsAfterTypeResolve().map { bound ->
            bound.declaredUpperBoundConeTypeOrNull() ?: return false
        }
        val remainingBounds = second.lookupTag.declaredUpperBoundRefsAfterTypeResolve().map { bound ->
            bound.declaredUpperBoundConeTypeOrNull() ?: return false
        }.toMutableList()
        if (firstBounds.size != remainingBounds.size) return false
        return firstBounds.all { bound ->
            val index = remainingBounds.indexOfFirst { areIdentical(bound, it) }
            if (index < 0) return@all false
            remainingBounds.removeAt(index)
            true
        }
    }

    private fun ConeCangJieType.isEnumSignatureType(): Boolean = when (this) {
        is ConeEnumType -> true
        is ConeClassLikeType -> classId == StdlibClassIds.Option ||
                session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir is CfirEnum
        else -> false
    }
}
