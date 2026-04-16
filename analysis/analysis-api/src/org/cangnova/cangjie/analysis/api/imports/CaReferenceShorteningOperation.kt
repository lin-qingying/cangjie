package org.cangnova.cangjie.analysis.api.imports

import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjExpression

interface CaReferenceShorteningOperation : CaLifetimeOwner {
    val expression: CjExpression

    val target: CaSymbol

    val shortName: Name

    val decision: CaCompletionCandidateDecision
}
