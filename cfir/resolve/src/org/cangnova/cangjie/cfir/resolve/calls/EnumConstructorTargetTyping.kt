package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
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
 * 判断候选是否对应源码中的裸无参 enum value 访问。
 *
 * 仓颉无参 enum case 在源码中是 value access；只有写出实际的函数调用节点时，
 * 才应按 constructor call 检查参数与返回值适用性。调用解析目前为了复用 enum
 * constructor 的 tower 查找而保留 [CallKind.EnumConstructorCall]，因此把这项
 * 语言级形态集中在候选工具中，供各个 resolution stage 使用，避免每个 stage
 * 依据节点类型重复推断或把显式 `Entry()` 误判为裸值。
 */
internal fun Candidate.isBareNoArgumentEnumValueAccess(): Boolean {
    if (callInfo.callKind != CallKind.EnumConstructorCall) return false
    val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
    return enumConstructor.valueParameters.isEmpty() &&
            callInfo.arguments.isEmpty() &&
            !callInfo.hasExplicitTypeArguments &&
            callInfo.callSite !is CfirFunctionCall
}

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

    val enumConstructorSymbol = when (reference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirResolvedErrorReference -> reference.resolvedSymbol
        else -> null
    } as? CfirEnumConstructorSymbol ?: return null
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
