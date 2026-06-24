package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedNamedReferenceImpl
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.name.Name


@OptIn(CfirImplementationDetail::class)
/**
 * 构造带最终替换结果的 resolved callable reference。
 *
 * 该引用保留候选符号、替换后的返回类型和参数类型，供后续 callable reference 适配与 analysis API 读取。
 */
internal fun buildAppliedCallableReference(
    name: Name,
    candidate: Candidate,
    substitutedReturnType: ConeCangJieType,
    finalSubstitutor: ConeSubstitutor,
): CfirResolvedNamedReference {
    if (!candidate.symbol.isBound) {
        return CfirResolvedNamedReferenceImpl(null, name, candidate.symbol)
    }

    val substitutedParameterTypes = when (candidate.symbol.cfir) {
        is CfirFunction,
        is CfirConstructor,
        is CfirEnumConstructor,
        -> candidate.declaredParametersForMapping().mapNotNull { parameter: CfirValueParameter ->
            val paramType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
            finalSubstitutor.substituteOrNull(candidate.substitutor.substituteOrSelf(paramType))
                ?: candidate.substitutor.substituteOrSelf(paramType)
        }
        else -> emptyList()
    }
    return CfirResolvedAppliedCallableReference(
        source = null,
        name = name,
        resolvedSymbol = candidate.symbol,
        substitutedReturnType = substitutedReturnType,
        substitutedParameterTypes = substitutedParameterTypes,
    )
}
