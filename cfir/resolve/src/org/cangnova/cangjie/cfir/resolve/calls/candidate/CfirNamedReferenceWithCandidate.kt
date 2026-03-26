package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

open class CfirNamedReferenceWithCandidate(
    override val source: CjSourceElement?,
    override val name: Name,
    val candidate: Candidate
) : CfirNamedReferenceWithCandidateBase() {
    override val candidateSymbol: CfirSymbol<*>
        get() = candidate.symbol

    open val isError: Boolean get() = false

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }
}

class CfirErrorReferenceWithCandidate(
    source: CjSourceElement?,
    name: Name,
    candidate: Candidate,
    val diagnostic: ConeDiagnostic
) : CfirNamedReferenceWithCandidate(source, name, candidate) {
    override val isError: Boolean get() = true
}
