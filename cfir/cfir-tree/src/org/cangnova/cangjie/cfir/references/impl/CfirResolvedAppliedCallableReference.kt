package org.cangnova.cangjie.cfir.references.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

class CfirResolvedAppliedCallableReference @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val name: Name,
    override val resolvedSymbol: CfirSymbol<*>,
    val substitutedReturnType: ConeCangjieType?,
    val substitutedParameterTypes: List<ConeCangjieType>,
) : CfirResolvedNamedReference() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(
        transformer: CfirTransformer<D>,
        data: D,
    ): CfirResolvedAppliedCallableReference {
        return this
    }
}
