package org.cangnova.cangjie.analysis.api.completion

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * Analysis API 对补全候选的稳定语义判断结果。
 *
 * 这里不直接生成 IDE 特定的 lookup element，而是先给出一层与平台无关的
 * “当前符号在当前位置是否可直接补全、是否需要额外导入”的判定快照。
 */
enum class CaCompletionCandidateStatus {
    /**
     * 当前符号可直接作为补全候选暴露，不需要新增导入。
     */
    DIRECT,

    /**
     * 当前符号需要在补全插入时显式补一条导入后才可稳定引用。
     */
    REQUIRES_IMPORT,

    /**
     * 当前符号在该位置不应作为补全候选暴露。
     */
    HIDDEN,
}

/**
 * 单个符号在指定位置上的补全可用性决策。
 */
interface CaCompletionCandidateDecision : CaLifetimeOwner {
    /**
     * 参与判定的公开符号。
     */
    val symbol: CaSymbol

    /**
     * 补全候选判定结果。
     */
    val status: CaCompletionCandidateStatus

    /**
     * 当 [status] 为 [CaCompletionCandidateStatus.REQUIRES_IMPORT] 时应补入的导入。
     */
    val requiredImport: ImportPath?
}
