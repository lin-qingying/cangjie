package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.instantiatedQualifierOwnerType
import org.cangnova.cangjie.cfir.declarations.noArgEnumConstructorTargetType
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
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition

/**
 * 同一个 enum owner 的目标类型按不变类型实参进入求解。
 * 直接比较 Option<T> 与 Option<Option<A>> 会先触发表达式装箱并误解出 T=A；
 * owner 映射应为 T=Option<A>，装箱只能在完成后的表达式类型检查中发生。
 */
internal fun Candidate.addExpectedEnumOwnerConstraints(
    initialType: ConeCangJieType,
    expectedType: ConeCangJieType,
    session: CfirSession,
): Boolean {
    if (symbol.takeIf { it.isBound }?.cfir !is CfirEnumConstructor || callInfo.hasExplicitTypeArguments) return false
    if (!shouldUseExpectedTypeForEnumConstructor(expectedType, session)) return false
    val initial = initialType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
    val expected = expectedType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
    if (initial.expandedClassIdOrPrimitiveClassId != expected.expandedClassIdOrPrimitiveClassId) return false
    if (initial.typeArguments.size != expected.typeArguments.size) return false
    for ((argument, expectedArgument) in initial.typeArguments.zip(expected.typeArguments)) {
        system.addEqualityConstraint(argument.type, expectedArgument.type, ConeExpectedTypeConstraintPosition)
    }
    return true
}

/**
 * enum 的返回目标只有在同一 owner 中才参与泛型推断。
 * Option 额外装箱由表达式检查负责：官方 LocalTypeArgumentSynthesis 不把额外外壳
 * 写入已有确定 payload 的泛型映射。本判定同时供实参检查前的 stage 和 completion 使用。
 */
internal fun Candidate.shouldUseExpectedTypeForEnumConstructor(
    expectedType: ConeCangJieType,
    session: CfirSession,
): Boolean {
    val constructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return true
    val ownerType = constructor.returnTypeRef.coneTypeOrNull?.fullyExpandedType(session) as? ConeLookupTagBasedType
        ?: return true
    val expected = expectedType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
    if (ownerType.expandedClassIdOrPrimitiveClassId != expected.expandedClassIdOrPrimitiveClassId) return false
    if (ownerType.expandedClassIdOrPrimitiveClassId != StdlibClassIds.Option ||
        constructor.valueParameters.isEmpty() || callInfo.hasExplicitTypeArguments || !argumentMappingInitialized
    ) return true
    val expectedPayload = expected.typeArguments.singleOrNull()?.type ?: return true
    val payload = argumentMapping.keys.singleOrNull()?.expression?.coneTypeOrNull ?: return true
    if (payload.contains { it is ConeTypeVariableType || it is ConeErrorType }) return true
    val expectedDepth = expectedPayload.optionNestingDepth(session) ?: return true
    val actualDepth = payload.optionNestingDepth(session) ?: return true
    return expectedDepth <= actualDepth
}

/** 统计展开别名后的 Option 外壳；不完整的裸泛型类型不提供层数证据。 */
private fun ConeCangJieType.optionNestingDepth(session: CfirSession): Int? {
    val expanded = fullyExpandedType(session) as? ConeLookupTagBasedType ?: return 0
    if (expanded.expandedClassIdOrPrimitiveClassId != StdlibClassIds.Option) return 0
    val inner = expanded.typeArguments.singleOrNull()?.type ?: return null
    return inner.optionNestingDepth(session)?.let { it + 1 }
}

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
    if (enumConstructorSymbol.instantiatedQualifierOwnerType(callInfo.explicitReceiver, session) != null) return null
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
    val qualifier = (this as? CfirQualifiedAccessExpression)?.explicitReceiver
    if (enumConstructorSymbol.instantiatedQualifierOwnerType(qualifier, session) != null) return null
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
