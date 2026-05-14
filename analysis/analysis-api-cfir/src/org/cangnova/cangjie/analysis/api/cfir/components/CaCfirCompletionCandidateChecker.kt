package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaCompletionCandidateChecker
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjElement

/**
 * 补全候选判定组件。
 */
internal class CaCfirCompletionCandidateChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaCompletionCandidateChecker {
    override fun CaSymbol.checkCompletionCandidate(position: CjElement): CaCompletionCandidateDecision = withValidityAssertion {
        analysisSession.checkCompletionCandidate(this@checkCompletionCandidate, position)
    }
}
