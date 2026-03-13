package org.cangnova.cangjie.cfir.resolve.body

/**
 * 候选验证管线执行器。
 *
 * 负责对调用解析中的每个候选（Candidate）执行一系列验证阶段（ResolutionStage），
 * 判定候选的适用性（Applicability）。
 *
 * Phase 2 仅提供骨架实现（无实际验证阶段），后续将逐步添加：
 * - CheckExtensionReceiver
 * - CheckDispatchReceiver
 * - MapArguments
 * - CheckArguments
 * 等验证阶段。
 *
 * 参考 K2 ResolutionStageRunner。
 */
class CfirResolutionStageRunner {

    /**
     * 对候选执行验证管线。
     *
     * Phase 2 直接返回 RESOLVED（所有候选都视为适用），
     * 后续将添加实际的阶段验证逻辑。
     */
    fun processCandidate(
        candidate: CfirCandidate,
        stopOnFirstError: Boolean = true,
    ): CfirCandidateApplicability {
        // Phase 2: 无验证阶段，所有候选直接通过
        return CfirCandidateApplicability.RESOLVED
    }
}

/**
 * 候选适用性等级。
 *
 * 参考 K2 CandidateApplicability。
 */
enum class CfirCandidateApplicability {
    /** 候选被隐藏（不可见） */
    HIDDEN,
    /** 候选不适用 */
    INAPPLICABLE,
    /** 候选适用（通过所有验证阶段） */
    RESOLVED,
}

/**
 * 调用解析候选。
 *
 * 封装一个候选函数符号及其验证结果。
 * Phase 2 为简化骨架，后续将添加：参数映射、类型推断变量、诊断信息等。
 *
 * 参考 K2 Candidate。
 */
class CfirCandidate(
    val symbol: org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol,
) {
    var applicability: CfirCandidateApplicability = CfirCandidateApplicability.RESOLVED
}
