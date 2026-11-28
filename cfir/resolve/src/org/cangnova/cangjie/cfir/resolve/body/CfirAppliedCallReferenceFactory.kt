package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement


@OptIn(CfirImplementationDetail::class)
/**
 * 构造带最终替换结果的 resolved callable reference。
 *
 * 该引用保留候选符号、替换后的返回类型和参数类型，供后续 callable reference 适配与 analysis API 读取。
 */
internal fun buildAppliedCallableReference(
    name: Name,
    candidate: Candidate,
    callSiteType: ConeCangJieType,
    finalSubstitutor: ConeSubstitutor,
    source: CjSourceElement?,
    matchedParentType: CfirReferenceParentType,
): CfirResolvedNamedReference {
    check(candidate.symbol.isBound) { "A completed callable reference requires its bound declaration" }

    val declaration = candidate.symbol.cfir
    val parameters = if (candidate.callInfo.callKind == CallKind.NamedValueAccess &&
        declaration !is CfirFunction && declaration !is CfirEnumConstructor) emptyList()
        else candidate.declaredParametersForMapping()
    val substitutedParameterTypes = parameters.map { parameter: CfirValueParameter ->
            val paramType = checkNotNull((parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType) {
                "A completed callable reference requires every declared parameter type"
            }
            finalSubstitutor.substituteOrSelf(candidate.substitutor.substituteOrSelf(paramType))
    }
    return CfirResolvedAppliedCallableReference(
        source = source,
        name = name,
        resolvedSymbol = candidate.symbol,
        substitutedReturnType = if (candidate.callInfo.callKind == CallKind.NamedValueAccess &&
            declaration !is CfirFunction && declaration !is CfirEnumConstructor) null
        else finalSubstitutor.substituteOrSelf(candidate.substitutedCallableSignatureReturnType()),
        substitutedParameterTypes = substitutedParameterTypes,
        isFunctionValue = candidate.callInfo.callKind == org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind.NamedValueAccess,
        matchedParentType = matchedParentType,
        callSiteType = callSiteType,
    )
}
