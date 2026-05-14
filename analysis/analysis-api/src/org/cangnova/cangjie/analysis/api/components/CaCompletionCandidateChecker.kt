package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjElement

/**
 * 补全候选检查协议。
 *
 * 设计要点/职责:
 * - 在给定位置上判定一个候选 symbol 是否适合作为补全项,并产出统一的 [CaCompletionCandidateDecision]。
 * - 不缓存候选结果,调用方负责按需复用决策。
 *
 * 对齐 Kotlin Analysis API 的 `KaCompletionCandidateChecker`,
 * 用于 IDE 补全场景下的可用性判断。
 */
interface CaCompletionCandidateChecker : CaLifetimeOwner {
    /**
     * 检查该 symbol 在 [position] 处是否可作为补全候选。
     */
    fun CaSymbol.checkCompletionCandidate(position: CjElement): CaCompletionCandidateDecision
}
