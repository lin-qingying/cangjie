package org.cangnova.cangjie.analysis.api.imports

import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjExpression

/**
 * 单条引用缩短操作。
 *
 * 把当前 [expression] 替换为 [shortName] 后,
 * 仍然解析到同一个 [target];是否需要伴随新增 import 由 [decision] 决定。
 */
interface CaReferenceShorteningOperation : CaLifetimeOwner {
    /** 待缩短的引用表达式。 */
    val expression: CjExpression

    /** 缩短后仍应解析到的目标符号。 */
    val target: CaSymbol

    /** 替换后写入源码的短名。 */
    val shortName: Name

    /** 缩短决定,包含可达性、是否需要 import 等。 */
    val decision: CaCompletionCandidateDecision
}
