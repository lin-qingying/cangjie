package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern

/**
 * match 分支可达性检查器。
 *
 * 对齐 C++ DiagKind::sema_unreachable_pattern:
 * 如果某一分支的模式被之前的分支(无 guard 的 wildcard `_` 或 simple binding `x`)
 * 完全覆盖,该分支及其后所有分支均不可达。
 *
 * 当前保守实现:只处理 wildcard / 无约束 binding pattern 两种"总匹配"模式。
 * 更复杂的穷尽性覆盖(enum 构造器集合覆盖等)由 Exhaustiveness checker 处理,
 * 此处不重叠覆盖。
 */
object CfirMatchUnreachablePatternChecker : CfirMatchExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val branches = expression.branches
        var coveredIndex = -1
        for ((i, branch) in branches.withIndex()) {
            if (coveredIndex >= 0) {
                reporter.reportOn(
                    source = branch.pattern.source ?: branch.source,
                    factory = CfirErrors.UNREACHABLE_PATTERN,
                )
                continue
            }
            if (branch.guard != null) continue
            val pat = branch.pattern
            if (pat is CfirWildcardPattern || pat is CfirBindingPattern) {
                coveredIndex = i
            }
        }
    }
}
