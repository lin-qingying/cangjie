package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirBasicExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirTryExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirTryHandleReturnChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckers

/**
 * 对齐 Kotlin `ExtraExpressionCheckers`。
 *
 * 仓颉当前额外表达式诊断主要来自 effects / try-handle 这组附加语义检查，
 * 因此先按现有主干 checker 归位到 extra 分组。
 */
object ExtraExpressionCheckers : ExpressionCheckers() {
    /** 额外表达式检查中面向基础表达式节点的 checker 集合。 */
    override val basicExpressionCheckers: Set<CfirBasicExpressionChecker> = emptySet()

    /** 额外表达式检查中面向 `try` 表达式节点的 checker 集合。 */
    override val tryExpressionCheckers: Set<CfirTryExpressionChecker> = setOf(
        CfirTryHandleReturnChecker,
    )
}
