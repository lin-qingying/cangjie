package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjElement

interface CaCompletionCandidateChecker : CaLifetimeOwner {
    fun CaSymbol.checkCompletionCandidate(position: CjElement): CaCompletionCandidateDecision
}
