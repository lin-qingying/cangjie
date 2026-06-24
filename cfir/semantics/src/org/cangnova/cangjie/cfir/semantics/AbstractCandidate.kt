package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * Cone 诊断使用的候选公共抽象。
 *
 * resolve 模块会把具体候选实现隐藏在该抽象之后，诊断层只依赖候选符号与适用性分类。
 */
abstract class AbstractCandidate {
    /** 当前候选代表的 CFIR 符号。 */
    abstract val symbol: CfirBasedSymbol<*>

    /** 当前候选在 tower resolve 中计算出的适用性。 */
    abstract val applicability: CandidateApplicability
}
