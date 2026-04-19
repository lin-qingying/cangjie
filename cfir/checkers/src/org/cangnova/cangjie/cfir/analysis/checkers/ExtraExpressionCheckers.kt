package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirBasicExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirEffectsBasicChecker
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
    override val basicExpressionCheckers: Set<CfirBasicExpressionChecker> = setOf(
        CfirEffectsBasicChecker,
    )

    override val tryExpressionCheckers: Set<CfirTryExpressionChecker> = setOf(
        CfirTryHandleReturnChecker,
    )
}
