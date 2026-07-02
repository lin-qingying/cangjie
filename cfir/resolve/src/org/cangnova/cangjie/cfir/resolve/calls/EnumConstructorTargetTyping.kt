package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.name.ClassId

/**
 * 按目标类型为无参 enum constructor 定型。
 *
 * 官方 `None` 这类无参 enum constructor 可以由目标 `Option<T>` 或同 owner enum 类型直接定型；
 * 这里把该规则集中在调用解析共享层，供实参检查与完成结果写回复用。
 */
internal fun Candidate.noArgEnumConstructorTargetType(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return null
    if (enumConstructor.valueParameters.isNotEmpty()) return null
    if (callInfo.hasExplicitTypeArguments) return null
    val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return null
    return enumConstructorSymbol.noArgEnumConstructorTargetType(expectedType, session)
}

/**
 * 若表达式已解析为无参 enum constructor 且目标类型匹配其 owner，则返回最终目标类型。
 */
internal fun CfirExpression.noArgEnumConstructorTargetType(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    if (this is CfirQualifiedAccessExpression && typeArguments.isNotEmpty()) return null

    val reference = (this as? CfirResolvable)?.calleeReference
    val candidate = reference as? CfirNamedReferenceWithCandidate
    if (candidate != null) {
        return candidate.candidate.noArgEnumConstructorTargetType(expectedType, session)
    }

    val enumConstructorSymbol = (reference as? CfirResolvedNamedReference)?.resolvedSymbol as? CfirEnumConstructorSymbol
        ?: return null
    return enumConstructorSymbol.noArgEnumConstructorTargetType(expectedType, session)
}

/**
 * 把已解析错误引用恢复成普通 resolved 引用，并写回目标类型。
 */
internal fun CfirExpression.applyNoArgEnumConstructorTargetType(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    val targetType = noArgEnumConstructorTargetType(expectedType, session) ?: return null
    replaceConeTypeOrNull(targetType)

    val resolvable = this as? CfirResolvable ?: return targetType
    when (val reference = resolvable.calleeReference) {
        is CfirResolvedErrorReference -> resolvable.replaceCalleeReference(
            buildResolvedNamedReference {
                source = reference.source
                name = reference.name
                resolvedSymbol = reference.resolvedSymbol
            }
        )

        is CfirNamedReferenceWithCandidate -> resolvable.replaceCalleeReference(
            buildResolvedNamedReference {
                source = reference.source
                name = reference.name
                resolvedSymbol = reference.candidateSymbol
            }
        )

        else -> Unit
    }
    return targetType
}

private fun CfirEnumConstructorSymbol.noArgEnumConstructorTargetType(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    val constructor = cfir
    if (constructor.valueParameters.isNotEmpty()) return null
    val ownerClassId = session.cfirProvider.getContainingClass(this)?.classId ?: return null
    val expandedExpectedType = expectedType.fullyExpandedType(session)
    return expandedExpectedType.takeIf {
        it.enumConstructorOwnerClassIdOrNull() == ownerClassId
    }
}

private fun ConeCangJieType.enumConstructorOwnerClassIdOrNull(): ClassId? = when (this) {
    is ConeEnumType -> classId
    is ConeClassLikeType -> classId.takeIf { it == StdlibClassIds.Option }
    else -> null
}
