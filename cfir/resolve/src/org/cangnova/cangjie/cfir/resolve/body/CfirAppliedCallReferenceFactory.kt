package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedNamedReferenceImpl
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.name.Name


@OptIn(CfirImplementationDetail::class)
internal fun buildAppliedCallableReference(
    name: Name,
    candidate: CfirCandidate,
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
            candidate.substitutor.substituteOrSelf(paramType)
        }
        else -> emptyList()
    }

    return CfirResolvedAppliedCallableReference(
        source = null,
        name = name,
        resolvedSymbol = candidate.symbol,
        substitutedReturnType = candidate.resolvedReturnType(),
        substitutedParameterTypes = substitutedParameterTypes,
    )
}
