package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.expectedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import java.util.Collections

/**
 * 调用完成时的真实实例化输入。实例化集合与是否为 generic call 分开保存，不能以替换器对象
 * 非空代替 TypeChecker::IsGenericCall。接收者及其继承图的泛型也属于 native maps.inst。
 */
private class CfirCallReferenceInstantiation(
    val isGenericCall: Boolean,
    val matchedParent: ConeCangJieType?,
    val parentSubstitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    val instantiations: Map<CfirTypeParameterSymbol, List<ConeCangJieType>>,
)

/** 完成写回器发布父类型，factory 只接收此完成产品，不重查 owner 或恢复实例化上下文。 */
internal fun completeReferenceParentType(candidate: Candidate, finalSubstitutor: ConeSubstitutor): CfirReferenceParentType {
    val input = captureReferenceInstantiation(candidate, finalSubstitutor)
    input.matchedParent?.let { parent ->
        val substitutor = substitutorByMap(input.parentSubstitutions, candidate.callInfo.session, allowIdenticalSubstitution = true)
        return CfirReferenceParentType.Instantiated(
            finalSubstitutor.substituteOrSelf(candidate.substitutor.substituteOrSelf(substitutor.substituteOrSelf(parent))),
        )
    }
    return if (input.isGenericCall && input.instantiations.isNotEmpty()) {
        CfirReferenceParentType.Invalid
    } else {
        CfirReferenceParentType.Absent
    }
}

private fun captureReferenceInstantiation(candidate: Candidate, finalSubstitutor: ConeSubstitutor): CfirCallReferenceInstantiation {
    check(candidate.symbol.isBound) { "A completed reference requires its bound candidate declaration" }
    val session = candidate.callInfo.session
    val callable = candidate.symbol as? CfirCallableSymbol<*>
    val original = callable?.unwrapFakeOverridesOrDelegated()
    val declaration = original?.cfir
    val ownerClass = original?.getContainingClass()
    val ownerExtend = original?.getContainingExtend()
    val parameters = linkedSetOf<CfirTypeParameterSymbol>()
    fun collectParameters(type: ConeCangJieType?) {
        type?.forEachType { if (it is ConeTypeParameterType) parameters += it.lookupTag.typeParameterSymbol }
    }
    (declaration as? CfirTypeParameterRefsOwner)?.typeParameters?.filterIsInstance<CfirTypeParameter>()
        ?.forEach { parameters += it.symbol }
    (candidate.symbol.cfir as? CfirTypeParameterRefsOwner)?.typeParameters?.filterIsInstance<CfirTypeParameter>()
        ?.forEach { parameters += it.symbol }
    (ownerClass?.cfir as? CfirTypeParameterRefsOwner)?.typeParameters?.forEach { parameters += it.symbol }
    ownerExtend?.typeParameters?.forEach { parameters += it.symbol }
    collectParameters((declaration as? CfirCallableDeclaration)?.returnTypeRef?.coneTypeOrNull)
    (declaration as? CfirFunction)?.valueParameters?.forEach { collectParameters(it.returnTypeRef.coneTypeOrNull) }
    candidate.freshVariables.forEach { variable ->
        (variable.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)?.let { parameters += it.typeParameterSymbol }
    }

    val receiver = candidate.dispatchReceiverExpression() ?: candidate.givenExtensionReceiver?.expression
    val receiverType = if (candidate.isFromOriginalTypeInPresenceOfSmartCast && receiver is CfirSmartCastExpression) {
        receiver.originalExpression.coneTypeOrNull
    } else receiver?.coneTypeOrNull
    val originalReceiverType = receiverType?.fullyExpandedType(session)
    val completedReceiverType = originalReceiverType?.let { finalSubstitutor.substituteOrSelf(candidate.substitutor.substituteOrSelf(it)) }
    val receiverDeclaration = completedReceiverType?.toSymbol(session)?.cfir as? CfirClassLikeDeclaration
    val usesUpperBound = when (originalReceiverType) {
        is ConeTypeParameterType -> true
        is ConeTypeVariableType -> originalReceiverType.typeConstructor.originalTypeParameter is ConeTypeParameterLookupTag
        else -> false
    }

    val instantiations = linkedMapOf<CfirTypeParameterSymbol, MutableList<ConeCangJieType>>()
    fun completed(type: ConeCangJieType): ConeCangJieType =
        finalSubstitutor.substituteOrSelf(candidate.substitutor.substituteOrSelf(type))
    fun addInstantiation(parameter: CfirTypeParameterSymbol, type: ConeCangJieType) {
        val actual = completed(type)
        val values = instantiations.getOrPut(parameter) { mutableListOf() }
        if (actual !in values) values += actual
    }

    // GenerateGenericMappingVisit 同时访问实际 receiver 和其已完成的继承类型。
    val pendingTypes = ArrayDeque<ConeCangJieType>()
    completedReceiverType?.let(pendingTypes::addLast)
    if (usesUpperBound) candidate.lookupProvenance.referenceContextType?.let(pendingTypes::addLast)
    val visited = linkedSetOf<ConeCangJieType>()
    while (pendingTypes.isNotEmpty()) {
        val type = completed(pendingTypes.removeFirst()).fullyExpandedType(session)
        if (!visited.add(type)) continue
        val symbol = type.toSymbol(session) as? CfirClassLikeSymbol<*>
        if (symbol != null && type is ConeLookupTagBasedType) {
            val ownerParameters = symbol.cfir.typeParameters
            parameters += ownerParameters.map { it.symbol }
            if (ownerParameters.size == type.typeArguments.size) {
                ownerParameters.zip(type.typeArguments).forEach { (parameter, argument) ->
                    addInstantiation(parameter.symbol, argument.type)
                }
            }
        }
        session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(type)?.forEach(pendingTypes::addLast)
    }

    val scope = candidate.originScope as? CfirClassSubstitutionScope
    val parentSubstitutions = linkedMapOf<CfirTypeParameterSymbol, ConeCangJieType>()
    for (parameter in parameters) {
        if (callable != null) scope?.substitutedCallableOwnerTypeParameterOrNull(callable, parameter)?.let { type ->
            parentSubstitutions[parameter] = completed(type)
            addInstantiation(parameter, type)
        }
        candidate.substitutor.substituteOrNull(parameter.constructType())?.let { type ->
            parentSubstitutions[parameter] = finalSubstitutor.substituteOrSelf(type)
            addInstantiation(parameter, type)
        }
    }

    val isFunctionCall = candidate.callInfo.callKind != CallKind.NamedValueAccess &&
        (declaration is CfirFunction || declaration is CfirEnumConstructor)
    val signatureHasGeneric = (declaration as? CfirTypeParameterRefsOwner)?.typeParameters?.filterIsInstance<CfirTypeParameter>()?.isNotEmpty() == true ||
        (declaration as? CfirCallableDeclaration)?.returnTypeRef?.coneTypeOrNull?.contains { it is ConeTypeParameterType } == true ||
        (declaration as? CfirFunction)?.valueParameters?.any { it.returnTypeRef.coneTypeOrNull?.contains { type -> type is ConeTypeParameterType } == true } == true
    val hasMemberReceiver = receiver != null
    val receiverIsGeneric = receiverDeclaration?.typeParameters?.isNotEmpty() == true &&
        (receiverDeclaration !is CfirEnum || declaration is CfirEnumConstructor || (declaration as? CfirCallableDeclaration)?.status?.isStatic == true)
    val nominalOwnerIsGeneric = ownerClass?.cfir?.let { it !is CfirBuiltInDeclaration && it.typeParameters.isNotEmpty() } == true
    val isSuperGenericConstructor = candidate.callInfo.origin.isSuperConstructorDelegation && nominalOwnerIsGeneric
    val isGenericCall = isFunctionCall && (signatureHasGeneric || candidate.callInfo.typeArguments.isNotEmpty() ||
        isSuperGenericConstructor || hasMemberReceiver && (receiverIsGeneric || nominalOwnerIsGeneric))

    val matchedFunctionValueType = usesUpperBound && declaration is CfirFunction &&
        candidate.callInfo.callKind == CallKind.NamedValueAccess &&
        candidate.resultingTypeForCallableReference != null &&
        candidate.callInfo.resolutionMode.expectedType?.fullyExpandedType(session) is ConeFunctionType
    val initialParent = when {
        isFunctionCall && ownerClass is CfirInterfaceSymbol -> candidate.lookupProvenance.requirementInterfaceType ?: ownerClass.constructType(
            ownerClass.cfir.typeParameters.map { it.symbol.constructType() },
        )
        matchedFunctionValueType && ownerClass != null -> ownerClass.constructType(
            ownerClass.cfir.typeParameters.map { it.symbol.constructType() },
        )
        matchedFunctionValueType && ownerExtend != null -> checkNotNull(ownerExtend.extendedTypeRef.coneTypeOrNull)
        usesUpperBound -> checkNotNull(candidate.lookupProvenance.referenceContextType) {
            "Completed upper-bound reference requires its actual lookup parent context"
        }
        else -> null
    }
    return CfirCallReferenceInstantiation(
        isGenericCall, initialParent, Collections.unmodifiableMap(parentSubstitutions),
        Collections.unmodifiableMap(instantiations.mapValues { Collections.unmodifiableList(it.value.toList()) }),
    )
}
