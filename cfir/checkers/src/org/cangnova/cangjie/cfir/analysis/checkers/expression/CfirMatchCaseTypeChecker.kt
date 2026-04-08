package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * `match { ... }`（无 selector）分支类型检查。
 *
 * 对齐官方 `ChkMatchCaseNoSelector` / `CheckMatchExprNoSelectorExhaustiveness` 的最小语义：
 * 对无主语 match，仅当分支本身是“条件表达式 case”并且该分支体没有得到稳定类型时，
 * 才报告 `MATCH_CASE_HAS_NO_TYPE`。
 *
 * 这里刻意不处理有 selector 的 pattern-match，因为那部分语义由 pattern legality /
 * exhaustiveness 以及后续 body resolve 共同负责。
 */
object CfirMatchCaseTypeChecker : CfirMatchExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        if (expression.subject != null) return

        expression.branches.forEach { branch ->
            val pattern = branch.pattern
            if (pattern !is CfirExpressionPattern && pattern !is CfirWildcardPattern) return@forEach

            val branchType = branch.body.coneTypeOrNull ?: branch.coneTypeOrNull
            if (branchType == null || branchType is ConeErrorType) {
                reporter.reportOn(
                    source = branch.source ?: pattern.source,
                    factory = CfirErrors.MATCH_CASE_HAS_NO_TYPE,
                )
            }
        }
    }
}
