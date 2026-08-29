package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeRigidType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 按 Range 表达式的外层 expected type 计算其真正的 `Range<T>` 类型。
 *
 * 官方 `ChkRangeExpr` 对直接 `Range<T>` 目标读取 `T`；对接口目标则以
 * `Range<自身类型参数>` 为模板沿继承图找到接口视图，再把接口实参反向统一
 * 回 Range 的类型参数。该逻辑是 Range resolve 与调用完成写回的共同语义，
 * 因此放在 providers 层，避免两个入口各自维护一套继承反推实现。
 */
fun ConeCangJieType.rangeTypeForExpectedType(session: CfirSession): ConeClassifierType? {
    val expandedExpectedType = fullyExpandedType(session)
    expandedExpectedType.directRangeTypeOrNull()?.let { return it }

    val expectedInterfaceType = expandedExpectedType as? ConeClassLikeType
        ?: return null
    if (!expectedInterfaceType.isInterface) return null

    val rangeSymbol = session.symbolProvider
        .getClassLikeSymbolByClassId(StdlibClassIds.Range)
        ?: return null
    val rangeTypeParameter = rangeSymbol.cfir.typeParameters
        .singleOrNull()
        ?.symbol
        ?.constructType()
        ?: return null
    val rangeType = rangeSymbol.constructType(listOf(rangeTypeParameter)) as? ConeRigidType
        ?: return null

    val expectedConstructor = with(session.typeContext) {
        expectedInterfaceType.typeConstructor()
    }
    val typeCheckerState = session.typeContext.newTypeCheckerState(
        errorTypesEqualToAnything = false,
        stubTypesEqualToAnything = false,
    )
    val correspondingSupertypes = AbstractTypeChecker.findCorrespondingSupertypes(
        typeCheckerState,
        rangeType,
        expectedConstructor,
    )

    val candidates = correspondingSupertypes.mapNotNull { correspondingSupertype ->
        val correspondingType = correspondingSupertype as? ConeCangJieType
            ?: return@mapNotNull null
        val rangeSubstitution = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
        if (!collectRangeTypeSubstitution(
                actualType = correspondingType,
                expectedType = expectedInterfaceType,
                rangeTypeParameter = rangeTypeParameter,
                session = session,
                substitution = rangeSubstitution,
            )
        ) {
            return@mapNotNull null
        }
        // 无参数接口（例如 `I`）会匹配 Range，但没有任何信息可以确定 T。
        // 官方 `Downgrade` 在这种情况下回退到按端点综合，不能把裸类型参数
        // 当作端点 expected type 向下传播。
        if (rangeSubstitution[rangeTypeParameter.lookupTag] == null) {
            return@mapNotNull null
        }
        CfirTypeSubstitutorByMap(rangeSubstitution)
            .substituteOrSelf(rangeType) as? ConeClassifierType
    }.distinct()

    return candidates.firstOrNull()
}

/** 直接识别 `Range<T>` 或其 typealias 展开结果。 */
private fun ConeCangJieType.directRangeTypeOrNull(): ConeClassifierType? = when (this) {
    is ConeClassLikeType -> takeIf { classId == StdlibClassIds.Range }
    is ConeStructType -> takeIf { classId == StdlibClassIds.Range }
    is ConeTypeAliasType -> expandedType?.directRangeTypeOrNull()
    else -> null
}

/** 将 `Range<T>` 的继承接口视图与外层接口目标统一，收集 `T` 的替换。 */
private fun collectRangeTypeSubstitution(
    actualType: ConeCangJieType,
    expectedType: ConeCangJieType,
    rangeTypeParameter: ConeTypeParameterType,
    session: CfirSession,
    substitution: MutableMap<TypeConstructorMarker, ConeCangJieType>,
): Boolean {
    val normalizedActualType = actualType.fullyExpandedType(session)
    val normalizedExpectedType = expectedType.fullyExpandedType(session)

    if (normalizedActualType is ConeTypeParameterType &&
        normalizedActualType.lookupTag == rangeTypeParameter.lookupTag
    ) {
        val previous = substitution.putIfAbsent(rangeTypeParameter.lookupTag, normalizedExpectedType)
        return previous == null ||
                AbstractTypeChecker.equalTypes(session.typeContext, previous, normalizedExpectedType)
    }
    if (normalizedActualType == normalizedExpectedType) return true

    if (normalizedActualType !is ConeLookupTagBasedType ||
        normalizedExpectedType !is ConeLookupTagBasedType ||
        normalizedActualType.lookupTag != normalizedExpectedType.lookupTag
    ) {
        return false
    }
    if (normalizedActualType.typeArguments.size != normalizedExpectedType.typeArguments.size) return false

    return normalizedActualType.typeArguments.zip(normalizedExpectedType.typeArguments).all { (actual, expected) ->
        collectRangeTypeSubstitution(
            actualType = actual.type,
            expectedType = expected.type,
            rangeTypeParameter = rangeTypeParameter,
            session = session,
            substitution = substitution,
        )
    }
}
