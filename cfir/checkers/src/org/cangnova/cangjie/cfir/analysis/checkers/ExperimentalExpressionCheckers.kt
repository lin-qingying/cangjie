package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckers

/**
 * 对齐 Kotlin `ExperimentalExpressionCheckers`。
 *
 * 仓颉当前主干尚未拆出独立的 experimental expression checker 分组，
 * 因而该容器保持为空，但作为稳定主接口参与 low-level diagnostics 组装。
 */
object ExperimentalExpressionCheckers : ExpressionCheckers()
