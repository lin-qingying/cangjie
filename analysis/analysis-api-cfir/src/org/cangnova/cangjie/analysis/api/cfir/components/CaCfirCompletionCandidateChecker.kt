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
    /**
     * 延迟取得当前 CFIR Analysis session，候选判定委托给 session 级导入规划实现。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaCompletionCandidateChecker {
    /**
     * 判断符号在指定位置作为补全候选时的可用状态。
     */
    override fun CaSymbol.checkCompletionCandidate(position: CjElement): CaCompletionCandidateDecision = withValidityAssertion {
        analysisSession.checkCompletionCandidate(this@checkCompletionCandidate, position)
    }
}
