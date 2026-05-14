package org.cangnova.cangjie.analysis.api.completion

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * 单个补全候选的判断结果。
 *
 * 把"目标符号 + 可达性 + 所需 import"三件事打包为不可变视图,
 * 供 IDE 补全管线、引用缩短等场景统一消费。
 */
interface CaCompletionCandidateDecision : CaLifetimeOwner {
    /** 候选项对应的目标符号。 */
    val symbol: CaSymbol

    /** 候选项可达性状态。 */
    val status: CaCompletionCandidateStatus

    /**
     * 若 [status] 为 [CaCompletionCandidateStatus.REQUIRES_IMPORT],
     * 这里携带需要追加的 import 路径;否则为 `null`。
     */
    val requiredImport: ImportPath?
}
