package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeMismatchError
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.commonSuperTypeOrNull
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * spawn 目标类型推断工具。
 *
 * 官方 `SynSpawnExpr` 在无目标类型时综合 `Future<taskReturn>`；
 * `ChkSpawnExpr` 在目标类型是 `Future<T>` 时把 task 按 `() -> T` 检查。
 * CFIR 的 body resolve 和调用实参检查都通过这里保持同一条语义路径。
 */
internal fun CfirSpawnExpression.synthesizeSpawnType(session: CfirSession): ConeCangJieType {
    val taskReturnType = spawnTaskReturnType(expectedTaskReturnType = null, session)
    // 官方 SynSpawnExpr：task 综合类型不是 well-typed（错误类型）时，整个 spawn 设为 invalid，
    // 而不是继续包装成名义的 Future<Error>。与 applySpawnExpectedFutureType 的错误传播保持一致。
    return if (taskReturnType is ConeErrorType) {
        ConeErrorType(ConeUnreportedDuplicateDiagnostic(taskReturnType.diagnostic))
    } else {
        constructFutureType(taskReturnType)
    }
}

/**
 * 按外层 `Future<T>` 目标类型检查 spawn task，并把最终类型写回 spawn 表达式。
 */
internal fun CfirSpawnExpression.applySpawnExpectedFutureType(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    val expectedFutureType = expectedType.futureTypeOrNull() ?: return null
    val expectedTaskReturnType = expectedFutureType.typeArguments.singleOrNull()?.type
        ?: return synthesizeSpawnType(session).also(::replaceConeTypeOrNull)

    val rawTaskReturnType = spawnTaskReturnType(expectedTaskReturnType, session)
    val actualTaskReturnType = IdealTypeResolver.resolveIfIdeal(rawTaskReturnType, expectedTaskReturnType)
    val resultType = when {
        actualTaskReturnType is ConeErrorType -> ConeErrorType(
            ConeUnreportedDuplicateDiagnostic(actualTaskReturnType.diagnostic),
            delegatedType = expectedFutureType,
        )

        AbstractTypeChecker.isSubtypeOf(session.typeContext, actualTaskReturnType, expectedTaskReturnType) == true ->
            expectedFutureType

        else -> ConeErrorType(
            ConeTypeMismatchError(
                ConeFunctionType(emptyList(), expectedTaskReturnType),
                ConeFunctionType(emptyList(), actualTaskReturnType),
            ),
            delegatedType = constructFutureType(actualTaskReturnType),
        )
    }

    replaceConeTypeOrNull(resultType)
    return resultType
}

/** 判断类型是否是标准库 `Future`，并在 typealias 场景下沿展开类型继续查找。 */
internal fun ConeCangJieType.futureTypeOrNull(): ConeClassifierType? = when (this) {
    is ConeClassLikeType -> takeIf { classId == StdlibClassIds.Future }
    is ConeTypeAliasType -> expandedType?.futureTypeOrNull()
    else -> null
}

/** 构造 `Future<returnType>` 的标准库 class-like 类型。 */
private fun constructFutureType(returnType: ConeCangJieType): ConeCangJieType =
    ConeClassLikeType(StdlibClassIds.Future.toLookupTag(), typeArguments = listOf(returnType))

/**
 * 综合 spawn task 的返回类型。
 *
 * 目标期望为 Unit 时只统计显式 `return 值` 的类型；否则把显式 return
 * 类型与 body 类型合并取公共超类型，无任何候选时回落到 Unit。
 */
private fun CfirSpawnExpression.spawnTaskReturnType(
    expectedTaskReturnType: ConeCangJieType?,
    session: CfirSession,
): ConeCangJieType {
    val explicitReturnTypes = body.collectSpawnTaskReturnTypes(session)
    if (expectedTaskReturnType?.isUnit == true) {
        val explicitValueReturnTypes = explicitReturnTypes.filterNot { it.isUnit || it.isNothing }
        return if (explicitValueReturnTypes.isEmpty()) {
            expectedTaskReturnType
        } else {
            session.commonSupertype(explicitValueReturnTypes)
        }
    }

    val bodyType = body.coneTypeOrNull
    val candidateTypes = explicitReturnTypes + listOfNotNull(bodyType)
    return if (candidateTypes.isEmpty()) session.builtinTypes.unitType else session.commonSupertype(candidateTypes)
}

/**
 * 收集 task body 中全部显式 `return` 表达式的类型。
 *
 * 只遍历当前 spawn 直接体：嵌套 lambda 与嵌套 spawn 不属于本次 task 的
 * 返回路径，遇到时整体跳过其子树；无 result 的 return 记为 Unit。
 */
private fun CfirBlock.collectSpawnTaskReturnTypes(session: CfirSession): List<ConeCangJieType> {
    val result = mutableListOf<ConeCangJieType>()
    acceptChildren(object : CfirVisitorVoid() {
        override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
            result += returnExpression.result?.coneTypeOrNull ?: session.builtinTypes.unitType
        }

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit

        override fun visitSpawnExpression(spawnExpression: CfirSpawnExpression) = Unit

        override fun visitElement(element: CfirElement) {
            element.acceptChildren(this, null)
        }
    }, null)
    return result
}

/**
 * 计算类型列表的公共超类型。
 *
 * 全部相同直接返回；过滤 Nothing 后无剩余取 Nothing、仅剩一个取该类型；
 * 真正多类型时交给类型系统求解，失败兜底为 Any，空列表回落为 Unit。
 */
private fun CfirSession.commonSupertype(types: List<ConeCangJieType>): ConeCangJieType {
    if (types.isEmpty()) return builtinTypes.unitType
    val first = types.first()
    if (types.all { it == first }) return first

    val nonNothing = types.filter { it != ConePrimitiveType.NOTHING }
    if (nonNothing.isEmpty()) return ConePrimitiveType.NOTHING
    if (nonNothing.size == 1) return nonNothing.first()

    return typeContext.commonSuperTypeOrNull(nonNothing) ?: ConeAnyType
}
